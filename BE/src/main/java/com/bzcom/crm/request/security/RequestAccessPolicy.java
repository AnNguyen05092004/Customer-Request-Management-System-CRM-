package com.bzcom.crm.request.security;

import com.bzcom.crm.auth.security.CurrentUser;
import com.bzcom.crm.common.exception.BusinessException;
import com.bzcom.crm.common.exception.ErrorCode;
import com.bzcom.crm.member.domain.MemberRole;
import com.bzcom.crm.request.entity.Request;

public final class RequestAccessPolicy {

    private RequestAccessPolicy() {}

    public static void assertCanRead(Request request, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (currentUser.role() == MemberRole.ADMIN) {
            return;
        }
        if (currentUser.role() == MemberRole.CLIENT && request.getClientId().equals(currentUser.memberId())) {
            return;
        }
        if (currentUser.role() == MemberRole.DEVELOPER
                && currentUser.memberId().equals(request.getAssignedDeveloperId())) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
}
