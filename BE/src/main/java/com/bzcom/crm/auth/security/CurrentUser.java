package com.bzcom.crm.auth.security;

import com.bzcom.crm.member.domain.MemberRole;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record CurrentUser(Long memberId, MemberRole role) {

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
