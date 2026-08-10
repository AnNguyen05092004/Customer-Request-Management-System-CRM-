package com.bzcom.crm.request.controller;

import com.bzcom.crm.auth.security.CurrentUser;
import com.bzcom.crm.common.response.ApiResponse;
import com.bzcom.crm.common.response.PageResponse;
import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestPriority;
import com.bzcom.crm.request.domain.RequestStatus;
import com.bzcom.crm.request.dto.request.RequestCreateRequest;
import com.bzcom.crm.request.dto.response.RequestResponse;
import com.bzcom.crm.request.dto.response.StatsResponse;
import com.bzcom.crm.request.service.RequestService;
import com.bzcom.crm.request.service.RequestStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/requests", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Request", description = "Quản lý yêu cầu khách hàng (CRUD, filter, gán, trạng thái)")
public class RequestController {

    private final RequestService requestService;
    private final RequestStatsService requestStatsService;

    public RequestController(RequestService requestService, RequestStatsService requestStatsService) {
        this.requestService = requestService;
        this.requestStatsService = requestStatsService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Tạo yêu cầu (CLIENT only)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Tạo request thành công",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Dữ liệu không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Chỉ CLIENT được tạo request",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<RequestResponse>> create(
            @Valid @RequestBody RequestCreateRequest request, @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(requestService.create(request, currentUser)));
    }

    @GetMapping
    @Operation(summary = "Danh sách request (phân trang / sort / filter tổ hợp; phạm vi theo role)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Trang kết quả",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Paging, sort hoặc filter không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<PageResponse<RequestResponse>> getRequests(
            @PageableDefault(page = 0, size = 10) Pageable pageable,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) RequestCategory category,
            @RequestParam(required = false) RequestPriority priority,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(requestService.getRequests(pageable, status, category, priority, keyword, currentUser));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết một request (kiểm quyền xem theo role)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Chi tiết request",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Không có quyền xem request",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy request",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<RequestResponse> getDetail(
            @PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(requestService.getRequestDetail(id, currentUser));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thống kê (ADMIN only) — tổng, completion rate, theo category, theo developer")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Dữ liệu thống kê",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Chỉ ADMIN được xem thống kê",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<StatsResponse> getStats() {
        return ApiResponse.ok(requestStatsService.getStats());
    }
}
