# LLM — Tính năng AI

> Thiết kế tính năng LLM cho **Bzcom CRM**. Rubric: *"purpose + context + output format rõ ràng"* ↔ *"prompt chung chung kiểu analyze this"*. → Tài liệu này trình bày **prompt có cấu trúc + output JSON + cách verify + mock mode**.
>
> Phục vụ **Slide 8 — LLM Feature**.

---

## Mục lục
1. [Chọn tính năng nào & vì sao](#1-chọn-tính-năng-nào--vì-sao)
2. [Nguyên tắc để đạt "Hire: YES"](#2-nguyên-tắc-để-đạt-hire-yes)
3. [Thiết kế prompt — auto-classify](#3-thiết-kế-prompt--auto-classify)
4. [Prompt phụ — suggest-priority & summary](#4-prompt-phụ--suggest-priority--summary)
5. [Xử lý output & verification](#5-xử-lý-output--verification)
6. [Mock mode (chống chết demo)](#6-mock-mode-chống-chết-demo)
7. [Trừu tượng hoá provider](#7-trừu-tượng-hoá-provider)
8. [Cấu hình & bảo mật](#8-cấu-hình--bảo-mật)
9. [Đối chiếu rubric & câu chốt PPT](#9-đối-chiếu-rubric--câu-chốt-ppt)

---

## 1. Chọn tính năng nào & vì sao

Đề cho chọn ≥1 trong 3:

| Tính năng | Endpoint | Độ khó | Giá trị demo |
|---|---|---|---|
| **Auto-classify** category | `POST /api/requests/classify` | Vừa | Cao — output enum rõ ràng, dễ verify đúng/sai |
| Suggest-priority | `POST /api/requests/suggest-priority` | Vừa | Trung bình |
| Auto-summary | `GET /api/requests/{id}/summary` | Dễ | Trung bình — khó chấm đúng/sai |

**Quyết định:** triển khai cả ba endpoint, nhưng **auto-classify** là tính năng trình bày chính ở slide 8. Ba endpoint dùng chung một `LlmService`, cùng cơ chế timeout/fallback/mock nên mở rộng giá trị demo mà không nhân ba hạ tầng AI.

> **Lý do (câu để nói trong PPT):** *"Chúng em lấy auto-classify làm điểm nhấn vì output là enum đóng (BUG/FEATURE/INQUIRY) nên verify được ngay; suggest-priority và summary tái sử dụng cùng guardrail để hoàn thiện trải nghiệm xử lý yêu cầu, không phải ba tính năng AI rời rạc."*

## 2. Nguyên tắc để đạt "Hire: YES"

Prompt phải có đủ 4 thành phần (thiếu 1 → rơi về "chung chung"):

| Thành phần | Trong dự án |
|---|---|
| **Purpose** (mục đích) | "Phân loại yêu cầu hỗ trợ của Bzcom vào đúng 1 nhãn" |
| **Context** (ngữ cảnh miền) | Định nghĩa rõ 3 nhãn BUG/FEATURE/INQUIRY theo nghiệp vụ Bzcom |
| **Output format** (định dạng) | JSON schema cố định, cấm chữ thừa |
| **Verification** (kiểm chứng) | Parse an toàn + fallback + confidence + con người xác nhận |

## 3. Thiết kế prompt — auto-classify

```
SYSTEM:
Bạn là trợ lý phân loại yêu cầu hỗ trợ của Bzcom — công ty vận hành web service.
Nhiệm vụ: phân loại mô tả của khách hàng vào ĐÚNG MỘT trong ba nhãn sau:
- BUG:     lỗi/sự cố khi hệ thống đang chạy (error, crash, không hoạt động đúng).
- FEATURE: đề nghị thêm hoặc cải tiến chức năng mới.
- INQUIRY: câu hỏi/thắc mắc, không phải lỗi cũng không phải yêu cầu tính năng.

Chỉ trả về JSON đúng định dạng sau, KHÔNG thêm bất kỳ chữ nào khác:
{"category":"BUG|FEATURE|INQUIRY","confidence":<0.0-1.0>,"reason":"<lý do ngắn gọn>"}

Nếu không chắc chắn, chọn nhãn khả dĩ nhất và hạ confidence xuống dưới 0.6.

FEW-SHOT (ví dụ mẫu):
Input: "Nút thanh toán bấm không phản hồi"
Output: {"category":"BUG","confidence":0.95,"reason":"chức năng không hoạt động"}
Input: "Cho tôi hỏi cách đổi mật khẩu?"
Output: {"category":"INQUIRY","confidence":0.90,"reason":"là câu hỏi hướng dẫn"}
Input: "Mong thêm đăng nhập bằng Google"
Output: {"category":"FEATURE","confidence":0.92,"reason":"đề nghị tính năng mới"}

USER:
Input: "<description của request>"
Output:
```

**Vì sao có few-shot?** Neo mô hình vào đúng 3 nhãn + đúng format JSON → giảm mạnh khả năng "bịa" nhãn hoặc trả văn xuôi. **Vì sao có confidence?** Để tầng sau quyết định có tin hay fallback (§5).

## 4. Prompt phụ — suggest-priority & summary

**suggest-priority** — output:
```json
{"priority":"HIGH|MEDIUM|LOW","confidence":0.0-1.0,"reason":"..."}
```
Context bổ sung trong system prompt: *"HIGH = ảnh hưởng nhiều người dùng / chặn nghiệp vụ / liên quan bảo mật-thanh toán; MEDIUM = ảnh hưởng một phần; LOW = mỹ phẩm/hỏi đáp."*

**summary** — output 1-2 câu tiếng Việt, không JSON, giới hạn độ dài. System prompt: *"Tóm tắt yêu cầu trong tối đa 2 câu, nêu vấn đề chính và mức khẩn cấp, không thêm thông tin không có trong mô tả."* (chống hallucination).

## 5. Xử lý output & verification

```
function classify(description):
    if !llmEnabled:                       # cấu hình tắt → mock
        return MockLlmService.classify(description)
    try:
        raw = llmClient.complete(buildPrompt(description))
        result = parseJson(raw)           # {category, confidence, reason}
        if result.category not in {BUG, FEATURE, INQUIRY}:
            throw ParseError
        return result
    catch (Timeout | ParseError | ApiError):
        return ruleBasedFallback(description)   # không bao giờ làm chết API

function ruleBasedFallback(desc):
    lower = desc.toLowerCase()
    if lower matches /(lỗi|error|crash|500|không hoạt động|fail)/: 
        return {BUG, 0.5, "keyword rule: error terms"}
    if lower matches /(thêm|mong|đề nghị|feature|hỗ trợ .* mới)/:
        return {FEATURE, 0.5, "keyword rule: request terms"}
    return {INQUIRY, 0.4, "fallback default"}
```

**Verification (điểm cộng lớn — nói trong PPT):**
- LLM chỉ **gợi ý** (BR-13). Kết quả trả về kèm `confidence` + `reason` để **con người / logic** xác nhận, không tự động ghi đè category do CLIENT chọn.
- Có **fallback rule** → dù LLM lỗi vẫn có kết quả hợp lý.
- Parse **an toàn** (try/catch, kiểm enum) → không để JSON rác làm 500.

## 6. Mock mode (chống chết demo)

`MockLlmService` trả kết quả **cố định theo keyword**, không gọi mạng:

```java
@Service
@ConditionalOnProperty(name = "llm.enabled", havingValue = "false", matchIfMissing = true)
public class MockLlmService implements LlmService {
    public ClassifyResult classify(String description) {
        String d = description.toLowerCase();
        if (d.contains("error") || d.contains("lỗi") || d.contains("500"))
            return new ClassifyResult(Category.BUG, 0.9, "[mock] error keyword");
        if (d.contains("thêm") || d.contains("feature") || d.contains("google"))
            return new ClassifyResult(Category.FEATURE, 0.9, "[mock] feature keyword");
        return new ClassifyResult(Category.INQUIRY, 0.8, "[mock] default");
    }
}
```

> **Chiến lược demo:** để `llm.enabled=false` (mock) khi demo trực tiếp → **không bao giờ chết vì mạng/API key**. Có thể bật `true` một lần để show LLM thật hoạt động, rồi tắt lại cho phần demo chính. Nói rõ điều này = thể hiện tư duy quản lý rủi ro.

## 7. Trừu tượng hoá provider

```java
public interface LlmService {
    ClassifyResult classify(String description);
    PriorityResult suggestPriority(String description);
    String summarize(RequestSummaryInput request);
}
```
| Impl | Kích hoạt | Vai trò |
|---|---|---|
| `MockLlmService` | `llm.enabled=false` (mặc định) | Demo an toàn, test cho cả 3 tác vụ |
| `OpenAiLlmService` | `llm.enabled=true` | Gọi OpenAI API thật cho cả 3 tác vụ |

Đổi sang Claude chỉ cần thêm `ClaudeLlmService implements LlmService` — controller/service không đổi (Dependency Inversion). Đây là ADR-08.

## 8. Cấu hình & bảo mật

```yaml
# application.yml
llm:
  enabled: ${LLM_ENABLED:false}
  provider: openai
  model: gpt-4o-mini          # model nhỏ, rẻ, đủ cho phân loại
  timeout-ms: 5000
  api-key: ${OPENAI_API_KEY:} # đọc từ env, KHÔNG commit
```

- **API key qua biến môi trường**, không hardcode, không commit (`.env` trong `.gitignore`).
- **Timeout** 5s → LLM chậm không treo request (rơi vào fallback).
- Không gửi dữ liệu nhạy cảm/cá nhân ra LLM (chỉ description yêu cầu; đây cũng là scope control).
- Chọn model nhỏ (gpt-4o-mini) → rẻ, nhanh, đủ chính xác cho bài phân loại 3 nhãn.

## 9. Đối chiếu rubric & câu chốt PPT

| Yếu tố rubric | Đáp ứng bằng |
|---|---|
| Purpose rõ | System prompt nêu rõ vai trò & nhiệm vụ |
| Context miền | Định nghĩa 3 nhãn theo nghiệp vụ Bzcom + few-shot |
| Output format | JSON schema cố định + cấm chữ thừa |
| Verification | Parse an toàn + confidence + fallback + người xác nhận |

**Câu chốt cho slide 8:**
> *"Prompt của chúng em không phải 'phân loại giúp' chung chung — nó nêu rõ mục đích, định nghĩa từng nhãn theo nghiệp vụ Bzcom, ép output JSON có confidence, và LLM chỉ đóng vai trò gợi ý: mọi kết quả đều parse an toàn, có fallback bằng rule, và quyết định cuối vẫn do người xác nhận."*

**Demo output thật để đưa lên slide** (chạy trước, chụp lại):
```
Input : "When I click login, page shows 500 error"
Output: {"category":"BUG","confidence":0.94,"reason":"mentions error on action"}
```

---

*Endpoint & schema LLM trong đặc tả: [openapi.yaml](./openapi.yaml) (tag `LLM`). Luật "LLM chỉ gợi ý": [ANALYZE.md BR-13](./ANALYZE.md#7-business-rules-br).*
