package com.bzcom.crm.request.controller;

import com.bzcom.crm.auth.security.CurrentUser;
import com.bzcom.crm.common.response.ApiResponse;
import com.bzcom.crm.common.response.PageResponse;
import com.bzcom.crm.request.domain.RequestCategory;
import com.bzcom.crm.request.domain.RequestPriority;
import com.bzcom.crm.request.domain.RequestStatus;
import com.bzcom.crm.request.dto.request.RequestCreateRequest;
import com.bzcom.crm.request.dto.response.RequestResponse;
import com.bzcom.crm.request.service.RequestService;
import io.swagger.v3.oas.annotations.Operation;
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

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Tạo yêu cầu (CLIENT only)")
    public ResponseEntity<ApiResponse<RequestResponse>> create(
            @Valid @RequestBody RequestCreateRequest request, @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(requestService.create(request, currentUser)));
    }

    @GetMapping
    @Operation(summary = "Danh sách request (phân trang / sort / filter tổ hợp; phạm vi theo role)")
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
    public ApiResponse<RequestResponse> getDetail(
            @PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(requestService.getRequestDetail(id, currentUser));
    }
}
