package com.bzcom.crm.member.domain;

import java.util.Locale;

public final class MemberEmail {

    private MemberEmail() {}

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
