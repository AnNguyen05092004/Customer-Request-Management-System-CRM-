package com.bzcom.crm.member.controller;

import com.bzcom.crm.auth.security.CurrentUser;
import com.bzcom.crm.common.response.ApiResponse;
import com.bzcom.crm.member.dto.request.MemberCreateRequest;
import com.bzcom.crm.member.dto.response.MemberResponse;
import com.bzcom.crm.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/members", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Member", description = "Đăng ký và tra cứu thành viên")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Đăng ký CLIENT công khai (role luôn là CLIENT)")
    @SecurityRequirements
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Tạo member thành công",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Dữ liệu không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Email đã tồn tại",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ResponseEntity<ApiResponse<MemberResponse>> register(@Valid @RequestBody MemberCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(memberService.register(request)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy tất cả member (ADMIN only)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Danh sách member",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Không đủ quyền",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<List<MemberResponse>> getAll() {
        return ApiResponse.ok(memberService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết member (ADMIN hoặc chính member đó)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Chi tiết member",
                useReturnTypeSchema = true),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Không đủ quyền",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy member",
                content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<MemberResponse> getById(@PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(memberService.getById(id, currentUser));
    }
}
