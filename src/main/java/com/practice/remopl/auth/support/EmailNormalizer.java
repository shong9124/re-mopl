package com.practice.remopl.auth.support;

import java.util.Locale;

public final class EmailNormalizer {
    private EmailNormalizer() { }

    public static String normalize(String email) {
        return email == null ? null : email.toLowerCase(Locale.ROOT);
    }
}
