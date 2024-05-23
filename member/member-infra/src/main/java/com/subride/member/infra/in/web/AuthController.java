package com.subride.member.infra.in.web;

import com.subride.common.dto.ResponseDTO;
import com.subride.common.util.CommonUtils;
import com.subride.member.biz.domain.Member;
import com.subride.member.biz.usecase.inport.IAuthService;
import com.subride.member.infra.common.dto.*;
import com.subride.member.infra.exception.InfraException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
개발표준: 콘트롤러에는 Service와 Controller Helper 클래스만 사용함
*/
@Slf4j
@Tag(name = "Auth API", description = "인증/인가 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final IAuthService authService;
    private final AuthContollerHelper authContollerHelper;

    @Operation(operationId = "auth-signup", summary = "회원가입", description = "회원가입을 처리합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ResponseDTO<String>> signup(@RequestBody SignupRequestDTO signupRequestDTO) {
        try {
            authService.signup(authContollerHelper.getMemberFromRequest(signupRequestDTO),
                    authContollerHelper.getAccountFromRequest(signupRequestDTO));

            return ResponseEntity.ok(CommonUtils.createSuccessResponse(200, "회원가입 성공", "회원가입 성공"));
        } catch (InfraException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CommonUtils.createFailureResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonUtils.createFailureResponse(0, "서버 오류가 발생했습니다."));
        }
    }

    @Operation(operationId = "auth-login", summary = "로그인", description = "로그인 처리")
    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<JwtTokenDTO>> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        try {
            Member member = authService.login(loginRequestDTO.getUserId(), loginRequestDTO.getPassword());
            if (member != null) {
                JwtTokenDTO jwtTokenDTO = authContollerHelper.createToken(member);
                return ResponseEntity.ok(CommonUtils.createSuccessResponse(200, "로그인 성공", jwtTokenDTO));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonUtils.createFailureResponse(0, "로그인 실패"));
            }
        } catch (InfraException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CommonUtils.createFailureResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonUtils.createFailureResponse(0, "서버 오류가 발생했습니다."));
        }
    }

    @Operation(operationId = "validate-token", summary = "인증 토큰 검증", description = "인증 토큰을 검증합니다.")
    @PostMapping("/verify")
    public ResponseEntity<ResponseDTO<Integer>> validate(@RequestBody JwtTokenVarifyDTO jwtTokenVarifyDTO) {
        try {
            int result = authContollerHelper.checkAccessToken(jwtTokenVarifyDTO.getToken());
            Member member = authContollerHelper.getMemberFromToken(jwtTokenVarifyDTO.getToken());

            if (member == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonUtils.createFailureResponse(0, "사용자 없음"));
            }

            if (authService.validateMemberAccess(member)) {
                return ResponseEntity.ok(CommonUtils.createSuccessResponse(200, "토큰 검증 성공", result));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(CommonUtils.createFailureResponse(0, "접근 권한이 없습니다."));
            }
        } catch (InfraException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CommonUtils.createFailureResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonUtils.createFailureResponse(0, "서버 오류가 발생했습니다."));
        }
    }

    @Operation(operationId = "refresh-token", summary = "인증 토큰 갱신", description = "인증 토큰을 갱신합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ResponseDTO<JwtTokenDTO>> refresh(@RequestBody JwtTokenRefreshDTO jwtTokenRefreshDTO) {
        try {
            authContollerHelper.isValidRefreshToken(jwtTokenRefreshDTO.getRefreshToken());
            Member member = authContollerHelper.getMemberFromToken(jwtTokenRefreshDTO.getRefreshToken());
            if (member == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonUtils.createFailureResponse(0, "사용자 없음"));
            }

            JwtTokenDTO jwtTokenDTO = authContollerHelper.createToken(member);
            return ResponseEntity.ok(CommonUtils.createSuccessResponse(200, "토큰 갱신 성공", jwtTokenDTO));
        } catch (InfraException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CommonUtils.createFailureResponse(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonUtils.createFailureResponse(0, "서버 오류가 발생했습니다."));
        }
    }
}