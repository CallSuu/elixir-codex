package com.elixircodex.backend.auth;

import com.example.demo.Entity.User;
import com.example.demo.Repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthenticatedUserService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new AuthenticatedUserService(userRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 인증된_이메일에_매칭되는_유저가_있으면_id를_반환한다() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@example.com", null, Collections.emptyList()));
        User user = User.builder().id(42L).email("user@example.com").build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        Long ownerId = service.getCurrentUserId();

        assertThat(ownerId).isEqualTo(42L);
    }

    @Test
    void 인증_정보가_없으면_예외를_던진다() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> service.getCurrentUserId())
                .isInstanceOf(AuthenticatedUserException.class)
                .hasMessage("인증이 필요합니다");
    }

    @Test
    void 인증된_이메일에_매칭되는_유저가_없으면_예외를_던진다() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ghost@example.com", null, Collections.emptyList()));
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentUserId())
                .isInstanceOf(AuthenticatedUserException.class)
                .hasMessage("인증된 사용자를 찾을 수 없습니다");
    }
}
