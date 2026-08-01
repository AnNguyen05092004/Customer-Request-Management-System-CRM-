package com.bzcom.crm.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST("COMMON_400", HttpStatus.BAD_REQUEST, "Invalid request"),
    UNAUTHORIZED("AUTH_401", HttpStatus.UNAUTHORIZED, "Unauthorized"),
    FORBIDDEN("AUTH_403", HttpStatus.FORBIDDEN, "Forbidden"),
    RESOURCE_NOT_FOUND("COMMON_404", HttpStatus.NOT_FOUND, "Resource not found"),
    CONFLICT("COMMON_409", HttpStatus.CONFLICT, "Resource conflict"),
    NO_DEVELOPER_AVAILABLE("WORKFLOW_422", HttpStatus.UNPROCESSABLE_ENTITY, "No developer available"),
    INTERNAL_SERVER_ERROR("COMMON_500", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
