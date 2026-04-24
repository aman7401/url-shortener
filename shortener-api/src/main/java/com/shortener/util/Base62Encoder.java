package com.shortener.util;

public class Base62Encoder {

    private static final String CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;

    private Base62Encoder() {}

    public static String encode(long number) {
        if (number <= 0) throw new IllegalArgumentException("Sequence number must be positive");
        StringBuilder sb = new StringBuilder();
        while (number > 0) {
            sb.append(CHARACTERS.charAt((int) (number % BASE)));
            number /= BASE;
        }
        return sb.reverse().toString();
    }
}
