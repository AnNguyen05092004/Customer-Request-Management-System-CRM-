package com.bzcom.crm.request.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum RequestStatus {
    PENDING,
    IN_PROGRESS,
    DONE;

    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED = Map.of(
            PENDING, EnumSet.of(IN_PROGRESS),
            IN_PROGRESS, EnumSet.of(DONE),
            DONE, EnumSet.noneOf(RequestStatus.class));

    public boolean canTransitionTo(RequestStatus next) {
        if (next == null) {
            return false;
        }
        return ALLOWED.getOrDefault(this, EnumSet.noneOf(RequestStatus.class)).contains(next);
    }
}
