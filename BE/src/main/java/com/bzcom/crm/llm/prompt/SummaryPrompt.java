package com.bzcom.crm.llm.prompt;

public final class SummaryPrompt {

    public static final String SYSTEM = "Tóm tắt yêu cầu trong tối đa 2 câu, nêu vấn đề chính và mức khẩn cấp, "
            + "không thêm thông tin không có trong mô tả.";

    private SummaryPrompt() {}
}
