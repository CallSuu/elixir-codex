package com.elixircodex.backend.stack;

import com.elixircodex.backend.auth.AuthenticatedUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplementControllerTest {

    @Mock
    private SupplementVerificationService supplementVerificationService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;

    private SupplementController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new SupplementController(supplementVerificationService, authenticatedUserService);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    void 정상_요청이면_결과를_그대로_반환한다() {
        MultipartFile image = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        SupplementVerifyResponse expected = new SupplementVerifyResponse(1L, "오메가3", 85, true, false);
        when(supplementVerificationService.verify(1L, image)).thenReturn(expected);

        SupplementVerifyResponse response = controller.verify(image);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    void 이미지_누락시_예외가_그대로_전파된다() {
        MultipartFile emptyImage = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);
        when(supplementVerificationService.verify(any(), any()))
                .thenThrow(new SupplementVerificationException("업로드된 이미지가 없습니다"));

        assertThatThrownBy(() -> controller.verify(emptyImage))
                .isInstanceOf(SupplementVerificationException.class)
                .hasMessage("업로드된 이미지가 없습니다");
    }
}
