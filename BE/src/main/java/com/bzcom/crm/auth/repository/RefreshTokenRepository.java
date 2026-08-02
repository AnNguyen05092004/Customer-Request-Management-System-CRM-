package com.bzcom.crm.auth.repository;

import com.bzcom.crm.auth.entity.RefreshToken;
import com.bzcom.crm.member.entity.Member;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query("select token.member from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<Member> findMemberByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update RefreshToken token
               set token.revokedAt = :revokedAt
             where token.tokenHash = :tokenHash
               and token.revokedAt is null
               and token.expiresAt > :revokedAt
            """)
    int revokeForRotation(@Param("tokenHash") String tokenHash, @Param("revokedAt") Instant revokedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update RefreshToken token
               set token.revokedAt = :revokedAt
             where token.tokenHash = :tokenHash
               and token.revokedAt is null
            """)
    int revokeForLogout(@Param("tokenHash") String tokenHash, @Param("revokedAt") Instant revokedAt);
}
