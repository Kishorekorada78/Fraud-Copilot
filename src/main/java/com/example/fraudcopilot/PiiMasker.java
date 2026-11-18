package com.example.fraudcopilot;

import java.util.regex.Pattern;

public final class PiiMasker {

    // 13 to 19 digits, optionally separated by spaces or dashes
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");

    private PiiMasker() {
    }

    public static String mask(String text) {
        return CARD.matcher(text).replaceAll(m -> {
            String digits = m.group().replaceAll("[ -]", "");
            return "**** **** **** " + digits.substring(digits.length() - 4);
        });
    }
}