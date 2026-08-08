package com.bzcom.crm.llm.dto;

public record ClassifyResult(String category, double confidence, String reason) {}
