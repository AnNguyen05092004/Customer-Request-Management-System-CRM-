package com.bzcom.crm.member.service;

import com.bzcom.crm.auth.security.CurrentUser;
import com.bzcom.crm.common.exception.BusinessException;
import com.bzcom.crm.common.exception.ErrorCode;
import com.bzcom.crm.member.domain.MemberEmail;
import com.bzcom.crm.member.domain.MemberRole;
import com.bzcom.crm.member.dto.request.MemberCreateRequest;
import com.bzcom.crm.member.dto.response.MemberResponse;
import com.bzcom.crm.member.entity.Member;
import com.bzcom.crm.member.mapper.MemberMapper;
import com.bzcom.crm.member.repository.MemberRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    public MemberService(
            MemberRepository memberRepository, MemberMapper memberMapper, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.memberMapper = memberMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public MemberResponse register(MemberCreateRequest request) {
        String email = MemberEmail.normalize(request.email());
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Member member = new Member(
                email,
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                MemberRole.CLIENT);
        try {
            return memberMapper.toResponse(memberRepository.saveAndFlush(member));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getAll() {
        return memberRepository.findAll().stream().map(memberMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MemberResponse getById(Long id, CurrentUser currentUser) {
        if (currentUser.role() != MemberRole.ADMIN && !currentUser.memberId().equals(id)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return memberMapper.toResponse(
                memberRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)));
    }

    @Transactional
    public void recordDeveloperCompletion(Long developerId, Instant completedAt) {
        Member developer = memberRepository
                .findById(developerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (developer.getRole() != MemberRole.DEVELOPER) {
            throw new BusinessException(ErrorCode.CONFLICT, "Member is not a developer");
        }
        developer.recordCompletion(completedAt);
    }
}
