package com.bzcom.crm.auth.service;

import com.bzcom.crm.auth.config.RefreshTokenProperties;
import com.bzcom.crm.auth.dto.request.LoginRequest;
import com.bzcom.crm.auth.dto.response.TokenResponse;
import com.bzcom.crm.auth.entity.RefreshToken;
import com.bzcom.crm.auth.jwt.JwtProvider;
import com.bzcom.crm.auth.repository.RefreshTokenRepository;
import com.bzcom.crm.auth.security.OpaqueTokenService;
import com.bzcom.crm.common.exception.BusinessException;
import com.bzcom.crm.common.exception.ErrorCode;
import com.bzcom.crm.member.domain.MemberEmail;
import com.bzcom.crm.member.entity.Member;
import com.bzcom.crm.member.repository.MemberRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    // BCrypt hash used only to equalize the cost of unknown-email and wrong-password logins.
    private static final String DUMMY_PASSWORD_HASH = "$2y$10$9FEnUY37Ok54UtidIbFlyOyNct/GMaF9dG1.GtIemnW6vWTPyfnGW";

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final OpaqueTokenService opaqueTokenService;
    private final RefreshTokenProperties refreshTokenProperties;
    private final Clock clock;

    public AuthService(
            MemberRepository memberRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider,
            OpaqueTokenService opaqueTokenService,
            RefreshTokenProperties refreshTokenProperties,
            Clock clock) {
        this.memberRepository = memberRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.opaqueTokenService = opaqueTokenService;
        this.refreshTokenProperties = refreshTokenProperties;
        this.clock = clock;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository
                .findByEmail(MemberEmail.normalize(request.email()))
                .orElse(null);
        String passwordHash = member == null ? DUMMY_PASSWORD_HASH : member.getPassword();
        boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);
        if (member == null || !passwordMatches) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return issueTokenPair(member);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        String tokenHash = opaqueTokenService.hash(rawRefreshToken);
        Member member = refreshTokenRepository
                .findMemberByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        Instant now = clock.instant();
        if (refreshTokenRepository.revokeForRotation(tokenHash, now) != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return issueTokenPair(member);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.revokeForLogout(opaqueTokenService.hash(rawRefreshToken), clock.instant());
    }

    private TokenResponse issueTokenPair(Member member) {
        String rawRefreshToken = opaqueTokenService.generate();
        RefreshToken refreshToken = new RefreshToken(
                member,
                opaqueTokenService.hash(rawRefreshToken),
                clock.instant().plus(refreshTokenProperties.ttl()));
        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole());
        return new TokenResponse(accessToken, rawRefreshToken, member.getRole());
    }
}
