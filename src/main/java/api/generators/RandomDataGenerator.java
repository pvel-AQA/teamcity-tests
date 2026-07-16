package api.generators;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

public final class RandomDataGenerator {
    private static final Random RANDOM = new Random();

    private RandomDataGenerator() {

    }

    public static String randomString(int length) {
        char[] lowercaseAlphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();

        return RandomStringUtils.insecure().next(length, lowercaseAlphabet);
    }

    public static String randomSpecificString(String start, int length) {
        String alphanumeric = "abcdefghijklmnopqrstuvwxyz0123456789";

        return start + RandomStringUtils.insecure().next(length, alphanumeric);
    }

    public static String getIncorrectPassword(){
        String letters = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";

        return RandomStringUtils.insecure().next(3, letters).toUpperCase() +
                RandomStringUtils.insecure().next(3, letters).toLowerCase() +
                RandomStringUtils.insecure().next(3, digits)+ "!@#";
    }
}
