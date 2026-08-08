package com.bzcom.crm.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bzcom.crm.request.domain.RequestStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequestStatusTest {

    @Test
    @DisplayName("PENDING can transition only to IN_PROGRESS")
    void testPendingTransitions() {
        assertTrue(RequestStatus.PENDING.canTransitionTo(RequestStatus.IN_PROGRESS));
        assertFalse(RequestStatus.PENDING.canTransitionTo(RequestStatus.DONE));
        assertFalse(RequestStatus.PENDING.canTransitionTo(RequestStatus.PENDING));
        assertFalse(RequestStatus.PENDING.canTransitionTo(null));
    }

    @Test
    @DisplayName("IN_PROGRESS can transition only to DONE")
    void testInProgressTransitions() {
        assertTrue(RequestStatus.IN_PROGRESS.canTransitionTo(RequestStatus.DONE));
        assertFalse(RequestStatus.IN_PROGRESS.canTransitionTo(RequestStatus.PENDING));
        assertFalse(RequestStatus.IN_PROGRESS.canTransitionTo(RequestStatus.IN_PROGRESS));
        assertFalse(RequestStatus.IN_PROGRESS.canTransitionTo(null));
    }

    @Test
    @DisplayName("DONE is terminal and cannot transition to anything")
    void testDoneTransitions() {
        assertFalse(RequestStatus.DONE.canTransitionTo(RequestStatus.PENDING));
        assertFalse(RequestStatus.DONE.canTransitionTo(RequestStatus.IN_PROGRESS));
        assertFalse(RequestStatus.DONE.canTransitionTo(RequestStatus.DONE));
        assertFalse(RequestStatus.DONE.canTransitionTo(null));
    }
}
