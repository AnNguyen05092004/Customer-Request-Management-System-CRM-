package com.bzcom.crm.llm.service;

import com.bzcom.crm.llm.dto.request.RequestSummaryInput;
import com.bzcom.crm.llm.dto.response.ClassifyResult;
import com.bzcom.crm.llm.dto.response.PriorityResult;

public interface LlmService {

    ClassifyResult classify(String description);

    PriorityResult suggestPriority(String description);

    String summarize(RequestSummaryInput request);
}
