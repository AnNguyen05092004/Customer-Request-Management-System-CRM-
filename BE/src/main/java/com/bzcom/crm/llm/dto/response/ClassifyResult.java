package com.bzcom.crm.llm.dto.response;

import com.bzcom.crm.request.domain.RequestCategory;

public record ClassifyResult(RequestCategory category, double confidence, String reason) {}
