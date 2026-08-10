package com.bzcom.crm.alert;

import com.bzcom.crm.alert.dto.AlertResponse;
import com.bzcom.crm.auth.security.CurrentUser;
import com.bzcom.crm.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Danh sách alert của member hiện tại",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<List<AlertResponse>> getMyAlerts(
            @RequestParam(required = false) Boolean isRead, @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(alertService.getOwnAlerts(currentUser.memberId(), isRead));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu alert đã đọc")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Đánh dấu đã đọc thành công",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Alert không thuộc member hiện tại",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy alert",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser) {
        alertService.markRead(id, currentUser.memberId());
        return ApiResponse.ok(null);
    }
}
