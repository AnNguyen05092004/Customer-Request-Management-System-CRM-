package com.bzcom.crm.llm.controller;

import com.bzcom.crm.auth.security.CurrentUser;
import com.bzcom.crm.common.response.ApiResponse;
import com.bzcom.crm.llm.dto.request.DescriptionRequest;
import com.bzcom.crm.llm.dto.request.RequestSummaryInput;
import com.bzcom.crm.llm.dto.response.ClassifyResult;
import com.bzcom.crm.llm.dto.response.PriorityResult;
import com.bzcom.crm.llm.dto.response.SummaryResponse;
import com.bzcom.crm.llm.service.LlmService;
import com.bzcom.crm.request.dto.response.RequestResponse;
import com.bzcom.crm.request.service.RequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/requests", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "LLM", description = "Phân loại và gợi ý bằng AI")
public class LlmController {

    private final LlmService llmService;
    private final RequestService requestService;

    public LlmController(LlmService llmService, RequestService requestService) {
        this.llmService = llmService;
        this.requestService = requestService;
    }

    @PostMapping(value = "/classify", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Tự phân loại category từ description")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Kết quả phân loại",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Dữ liệu không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<ClassifyResult> classify(@Valid @RequestBody DescriptionRequest request) {
        return ApiResponse.ok(llmService.classify(request.description()));
    }

    @PostMapping(value = "/suggest-priority", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Gợi ý priority từ description")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Priority gợi ý",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Dữ liệu không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<PriorityResult> suggestPriority(@Valid @RequestBody DescriptionRequest request) {
        return ApiResponse.ok(llmService.suggestPriority(request.description()));
    }

    @GetMapping("/{id}/summary")
    @Operation(summary = "Tóm tắt 1-2 dòng cho một request")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Tóm tắt",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Không có quyền xem request này",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy request",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<SummaryResponse> summary(@PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser) {
        RequestResponse request = requestService.getRequestDetail(id, currentUser);
        String summary = llmService.summarize(new RequestSummaryInput(request.description()));
        return ApiResponse.ok(new SummaryResponse(summary));
    }
}
