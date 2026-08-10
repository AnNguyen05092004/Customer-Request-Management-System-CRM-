/**
 * Cung cấp tính năng AI (LLM) cho hệ thống CRM: tự động phân loại category,
 * đề xuất priority, và tóm tắt mô tả yêu cầu. Mặc định dùng {@link com.bzcom.crm.llm.service.MockLlmService}
 * (keyword-based, không cần API key). Bật {@code llm.enabled=true} và cấu hình
 * {@code llm.api-key} để dùng {@link com.bzcom.crm.llm.service.OpenAiLlmService} thật.
 */
package com.bzcom.crm.llm;
