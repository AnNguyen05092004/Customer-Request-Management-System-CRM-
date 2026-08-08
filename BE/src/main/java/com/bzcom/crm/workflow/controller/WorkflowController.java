package com.bzcom.crm.workflow.controller;

import com.bzcom.crm.auth.security.CurrentUser;
import com.bzcom.crm.common.response.ApiResponse;
import com.bzcom.crm.request.dto.response.RequestResponse;
import com.bzcom.crm.workflow.dto.request.AssignRequest;
import com.bzcom.crm.workflow.dto.request.StatusUpdateRequest;
import com.bzcom.crm.workflow.dto.response.HistoryResponse;
import com.bzcom.crm.workflow.service.HistoryService;
import com.bzcom.crm.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/requests", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Request", description = "Quy trình xử lý Yêu cầu (gán developer, trạng thái, lịch sử)")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final HistoryService historyService;

    public WorkflowController(WorkflowService workflowService, HistoryService historyService) {
        this.workflowService = workflowService;
        this.historyService = historyService;
    }

    @PatchMapping(value = "/{id}/assign", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Gán developer (ADMIN only). auto=true -> thuật toán tự chọn.")
    public ApiResponse<RequestResponse> assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignRequest request,
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(workflowService.assign(id, request, currentUser));
    }

    @PatchMapping(value = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cập nhật trạng thái; request phải được gán developer và transition phải hợp lệ -> 409")
    public ApiResponse<RequestResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(workflowService.updateStatus(id, request, currentUser));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Lịch sử thay đổi trạng thái của request")
    public ApiResponse<List<HistoryResponse>> getHistory(
            @PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(historyService.getHistory(id, currentUser));
    }
}
