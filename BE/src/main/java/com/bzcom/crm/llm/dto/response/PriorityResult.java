package com.bzcom.crm.llm.dto.response;

import com.bzcom.crm.request.domain.Priority;

public record PriorityResult(Priority priority, double confidence, String reason) {}
