package com.bzcom.crm.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bzcom.crm.common.exception.BusinessException;
import com.bzcom.crm.common.exception.ErrorCode;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Test
    void shouldListOnlyCurrentMembersAlertsAndFilterReadState() {
        Alert alert = new Alert(10L, 20L, AlertType.ASSIGNED, "Assigned to you");
        when(alertRepository.findByTargetMemberIdAndIsReadOrderByCreatedAtDesc(20L, false))
                .thenReturn(Stream.of(alert).toList());

        assertThat(new AlertService(alertRepository).getOwnAlerts(20L, false))
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.requestId()).isEqualTo(10L);
                    assertThat(response.alertType()).isEqualTo(AlertType.ASSIGNED);
                    assertThat(response.isRead()).isFalse();
                });
    }

    @Test
    void shouldMarkOwnAlertAsRead() {
        Alert alert = new Alert(10L, 20L, AlertType.STATUS_CHANGED, "Status updated");
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        new AlertService(alertRepository).markRead(1L, 20L);

        assertThat(alert.isRead()).isTrue();
    }

    @Test
    void shouldRejectMarkingAnotherMembersAlertAsRead() {
        Alert alert = new Alert(10L, 20L, AlertType.STATUS_CHANGED, "Status updated");
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> new AlertService(alertRepository).markRead(1L, 21L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @ParameterizedTest
    @EnumSource(AlertType.class)
    void shouldSaveEveryAlertType(AlertType type) {
        AlertService alertService = new AlertService(alertRepository);

        alertService.create(20L, 10L, type, "Alert message");

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetMemberId()).isEqualTo(20L);
        assertThat(captor.getValue().getRequestId()).isEqualTo(10L);
        assertThat(captor.getValue().getAlertType()).isEqualTo(type);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void shouldRejectBlankAlertMessage(String message) {
        assertThatThrownBy(() -> new AlertService(alertRepository).create(20L, 10L, AlertType.ASSIGNED, message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Alert message must not be blank");
    }

    @Test
    void shouldRejectOversizedAlertMessage() {
        assertThatThrownBy(
                        () -> new AlertService(alertRepository).create(20L, 10L, AlertType.ASSIGNED, "x".repeat(256)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Alert message must not exceed 255 characters");
    }
}
