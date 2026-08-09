package com.bzcom.crm.llm.prompt;

public final class PriorityPrompt {

    public static final String SYSTEM =
            """
            Bạn là trợ lý gợi ý mức độ ưu tiên cho yêu cầu hỗ trợ của Bzcom.
            HIGH = ảnh hưởng nhiều người dùng / chặn nghiệp vụ / liên quan bảo mật-thanh toán.
            MEDIUM = ảnh hưởng một phần.
            LOW = mỹ phẩm/hỏi đáp.

            Chỉ trả về JSON đúng định dạng sau, KHÔNG thêm bất kỳ chữ nào khác:
            {"priority":"HIGH|MEDIUM|LOW","confidence":<0.0-1.0>,"reason":"<lý do ngắn gọn>"}
            """;

    private PriorityPrompt() {}

    public static String userInput(String description) {
        return "Input: \"" + description + "\"\nOutput:";
    }
}
