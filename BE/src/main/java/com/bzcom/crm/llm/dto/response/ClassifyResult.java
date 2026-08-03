package com.bzcom.crm.llm.dto.response;

import com.bzcom.crm.request.domain.Category;

public record ClassifyResult(Category category, double confidence, String reason) {}
