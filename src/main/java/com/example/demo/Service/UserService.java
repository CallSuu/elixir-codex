package com.example.demo.Service;

import com.example.demo.Dto.UserRequestDto;
import com.example.demo.Entity.BlacklistedToken;
import com.example.demo.Entity.User;
import com.example.demo.Repository.BlacklistedTokenRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    // 1. 회원가입 (비밀번호 암호화 후 저장)
    @Transactional
    public String signup(UserRequestDto.SignUp requestDto) {
        if (userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        User user = User.builder()
                .email(requestDto.getEmail())
                .password(encodedPassword)
                .selectedCategory(requestDto.getSelectedCategory())
                .subscriptionStatus("FREE")
                .build();

        userRepository.save(user);
        return "회원가입이 완료되었습니다.";
    }

    // 2. 로그인 (세션 없이 JWT 토큰 발급 및 반환)
    @Transactional(readOnly = true)
    public String login(UserRequestDto.Login requestDto) {
        User user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return jwtTokenProvider.createToken(user.getEmail());
    }

    // 3. 로그아웃 (토큰을 만료 시각까지 블랙리스트에 등록해 즉시 무효화)
    @Transactional
    public void logout(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        if (blacklistedTokenRepository.existsByToken(token)) {
            return;
        }

        LocalDateTime expiresAt = jwtTokenProvider.getExpirationFromToken(token)
                .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        blacklistedTokenRepository.save(BlacklistedToken.builder()
                .token(token)
                .expiresAt(expiresAt)
                .build());
    }
}