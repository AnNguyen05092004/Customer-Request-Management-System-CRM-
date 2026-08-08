package com.bzcom.crm.llm;

import com.bzcom.crm.common.response.ApiResponse;
import com.bzcom.crm.llm.dto.ClassifyResult;
import com.bzcom.crm.llm.dto.PriorityResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@Tag(name = "LLM", description = "Tính năng AI hỗ trợ phân loại và tóm tắt yêu cầu")
public class LlmController {

    private final LlmService llmService;

    public record DescriptionRequest(String description) {}

    @PostMapping("/classify")
    @Operation(summary = "Tự động phân loại category từ mô tả")
    public ApiResponse<ClassifyResult> classify(@RequestBody DescriptionRequest request) {
        return ApiResponse.ok(llmService.classify(request.description()));
    }

    @PostMapping("/suggest-priority")
    @Operation(summary = "Tự động đề xuất priority từ mô tả")
    public ApiResponse<PriorityResult> suggestPriority(@RequestBody DescriptionRequest request) {
        return ApiResponse.ok(llmService.suggestPriority(request.description()));
    }

    @PostMapping("/summarize")
    @Operation(summary = "Tự động tóm tắt mô tả yêu cầu trong 1-2 câu")
    public ApiResponse<String> summarize(@RequestBody DescriptionRequest request) {
        return ApiResponse.ok(llmService.summarize(request.description()));
    }
}