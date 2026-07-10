package api.generators;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

public final class RandomDataGenerator {
    private static final Random RANDOM = new Random();

    private RandomDataGenerator() {

    }

    public static String randomString(int length) {
        String letters = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(letters.charAt(RANDOM.nextInt(letters.length())));
        }

        return sb.toString();
    }

    public static String randomSpecificString(String start, int length) {
        String letters = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(letters.charAt(RANDOM.nextInt(letters.length())));
        }

        return start + sb;
    }

    public static String getIncorrectPassword(){
        return RandomStringUtils.randomAlphanumeric(3).toUpperCase() +
                RandomStringUtils.randomAlphanumeric(3).toLowerCase() +
                RandomStringUtils.randomNumeric(2) + "!@#";
    }
}
