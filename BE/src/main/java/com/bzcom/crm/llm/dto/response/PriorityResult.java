package com.bzcom.crm.llm.dto.response;

import com.bzcom.crm.request.domain.RequestPriority;

public record PriorityResult(RequestPriority priority, double confidence, String reason) {}
