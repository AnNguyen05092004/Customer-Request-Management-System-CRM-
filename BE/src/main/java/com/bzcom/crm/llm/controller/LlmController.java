package com.bzcom.crm.llm.controller;

import com.bzcom.crm.common.response.ApiResponse;
import com.bzcom.crm.llm.dto.request.DescriptionRequest;
import com.bzcom.crm.llm.dto.response.ClassifyResult;
import com.bzcom.crm.llm.dto.response.PriorityResult;
import com.bzcom.crm.llm.service.LlmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/requests", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "LLM", description = "Phân loại và gợi ý bằng AI")
public class LlmController {

    private final LlmService llmService;

    public LlmController(LlmService llmService) {
        this.llmService = llmService;
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
}
