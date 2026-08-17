package com.example.demo.Controller;

import com.example.demo.Dto.UserRequestDto;
import com.example.demo.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 1. 회원가입 API
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody UserRequestDto.SignUp request) {
        String result = userService.signup(request);
        return ResponseEntity.ok(result);
    }

    // 2. 로그인 API (JWT 토큰 반환)
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody UserRequestDto.Login request) {
        String token = userService.login(request);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("tokenType", "Bearer");

        return ResponseEntity.ok(response);
    }

    // 3. 내 정보 조회 API (JWT 인증 필터 동작 테스트용)
    @GetMapping("/me")
    public ResponseEntity<String> me(Authentication authentication) {
        return ResponseEntity.ok(authentication.getName());
    }

    // 4. 로그아웃 API (토큰 블랙리스트 등록)
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authorizationHeader) {
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization 헤더 형식이 올바르지 않습니다.");
        }
        String token = authorizationHeader.substring(7);
        userService.logout(token);
        return ResponseEntity.ok("로그아웃되었습니다.");
    }
}