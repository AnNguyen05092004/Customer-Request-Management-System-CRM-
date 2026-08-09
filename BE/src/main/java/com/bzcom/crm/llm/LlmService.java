package com.bzcom.crm.llm;

import com.bzcom.crm.llm.dto.ClassifyResult;
import com.bzcom.crm.llm.dto.PriorityResult;

public interface LlmService {
    ClassifyResult classify(String description);

    PriorityResult suggestPriority(String description);

    String summarize(String description);
}
