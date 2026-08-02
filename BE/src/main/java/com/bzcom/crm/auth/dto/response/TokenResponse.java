package com.bzcom.crm.auth.dto.response;

import com.bzcom.crm.member.domain.MemberRole;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, MemberRole role) {

    public static final String BEARER_TOKEN_TYPE = "Bearer";

    public TokenResponse(String accessToken, String refreshToken, MemberRole role) {
        this(accessToken, refreshToken, BEARER_TOKEN_TYPE, role);
    }
}
