package com.bzcom.crm.member.dto.response;

import com.bzcom.crm.member.domain.MemberRole;
import java.time.Instant;

public record MemberResponse(Long id, String email, String name, MemberRole role, Instant createdAt) {}
