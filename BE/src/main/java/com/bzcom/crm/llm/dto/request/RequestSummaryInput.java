package com.bzcom.crm.llm.dto.request;

public record RequestSummaryInput(String description) {

    public static final String EMPTY_SUMMARY = "No description provided.";

    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }
}
