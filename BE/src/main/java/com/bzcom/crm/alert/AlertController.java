package com.bzcom.crm.alert;

import com.bzcom.crm.alert.dto.AlertResponse;
import com.bzcom.crm.auth.security.CurrentUser;
import com.bzcom.crm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert", description = "Quản lý thông báo cá nhân")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "Lấy danh sách alert của mình")
    public ApiResponse<List<AlertResponse>> getMyAlerts(
            @RequestParam(required = false) Boolean isRead, @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(alertService.getOwnAlerts(currentUser.memberId(), isRead));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu alert đã đọc")
    public ApiResponse<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser) {
        alertService.markRead(id, currentUser.memberId());
        return ApiResponse.ok(null);
    }
}
