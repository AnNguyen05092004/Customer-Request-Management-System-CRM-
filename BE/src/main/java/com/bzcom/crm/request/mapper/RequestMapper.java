package com.bzcom.crm.request.mapper;

import com.bzcom.crm.common.mapper.CentralMapperConfig;
import com.bzcom.crm.request.dto.response.RequestResponse;
import com.bzcom.crm.request.entity.Request;
import org.mapstruct.Mapper;

@Mapper(config = CentralMapperConfig.class, componentModel = "spring")
public interface RequestMapper {

    RequestResponse toResponse(Request request);
}
