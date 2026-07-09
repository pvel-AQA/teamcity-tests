package helpers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerLogsExtractor {

    public static final String DOCKER_HOST = getDockerHost();
    public static final Pattern SUPER_USER_AUTH_TOKEN_PATTERN =
            Pattern.compile("Super user authentication token:\\s*([a-f0-9-]+)");
    private final DockerClient dockerClient;

    public DockerLogsExtractor() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(DOCKER_HOST)
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();
        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    public Matcher extractByPatternFromContainer(Pattern pattern, String containerName, int logTail) throws Exception {
        try (dockerClient) {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CountDownLatch latch = new CountDownLatch(1);

            LogContainerCmd logCmd = dockerClient.logContainerCmd(containerName)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTail(logTail);

            logCmd.exec(new LogContainerResultCallback() {
                @Override
                public void onNext(com.github.dockerjava.api.model.Frame item) {
                    try {
                        outputStream.write(item.getPayload());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });

            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timeout reading logs");
            }

            String logs = outputStream.toString();
            return pattern.matcher(logs);
        }
    }

    private static String getDockerHost() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "npipe:////./pipe/docker_engine";
        } else {
            return "unix:///var/run/docker.sock";
        }
    }
}
