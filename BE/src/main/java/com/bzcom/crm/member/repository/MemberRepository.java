package com.bzcom.crm.member.repository;

import com.bzcom.crm.member.domain.MemberRole;
import com.bzcom.crm.member.entity.Member;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Member> findAllByRoleOrderByIdAsc(MemberRole role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select member from Member member where member.role = :role order by member.id asc")
    List<Member> findAllByRoleForUpdate(@Param("role") MemberRole role);
}
