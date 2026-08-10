package com.bzcom.crm.member.mapper;

import com.bzcom.crm.common.mapper.CentralMapperConfig;
import com.bzcom.crm.member.dto.response.MemberResponse;
import com.bzcom.crm.member.entity.Member;
import org.mapstruct.Mapper;

@Mapper(config = CentralMapperConfig.class, componentModel = "spring")
public interface MemberMapper {

    MemberResponse toResponse(Member member);
}
