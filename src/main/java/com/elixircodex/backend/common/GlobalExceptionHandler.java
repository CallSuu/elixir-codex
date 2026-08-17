package com.elixircodex.backend.common;

import com.elixircodex.backend.alchemy.AlchemyNameGenerationException;
import com.elixircodex.backend.alchemy.CodexValidationException;
import com.elixircodex.backend.alchemy.SynthesizeValidationException;
import com.elixircodex.backend.attendance.AttendanceValidationException;
import com.elixircodex.backend.auth.AuthenticatedUserException;
import com.elixircodex.backend.onboarding.OnboardingClassificationException;
import com.elixircodex.backend.onboarding.OnboardingValidationException;
import com.elixircodex.backend.specialelixir.SpecialElixirValidationException;
import com.elixircodex.backend.stack.StackValidationException;
import com.elixircodex.backend.stack.SupplementVerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthenticatedUserException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticatedUser(AuthenticatedUserException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return badRequest(e.getMessage());
    }

    @ExceptionHandler(StackValidationException.class)
    public ResponseEntity<Map<String, String>> handleStackValidation(StackValidationException e) {
        return badRequest(e.getMessage());
    }

    @ExceptionHandler(SynthesizeValidationException.class)
    public ResponseEntity<Map<String, String>> handleSynthesizeValidation(SynthesizeValidationException e) {
        return badRequest(e.getMessage());
    }

    @ExceptionHandler(AttendanceValidationException.class)
    public ResponseEntity<Map<String, String>> handleAttendanceValidation(AttendanceValidationException e) {
        return badRequest(e.getMessage());
    }

    @ExceptionHandler(CodexValidationException.class)
    public ResponseEntity<Map<String, String>> handleCodexValidation(CodexValidationException e) {
        return badRequest(e.getMessage());
    }

    @ExceptionHandler(OnboardingValidationException.class)
    public ResponseEntity<Map<String, String>> handleOnboardingValidation(OnboardingValidationException e) {
        return badRequest(e.getMessage());
    }

    @ExceptionHandler(OnboardingClassificationException.class)
    public ResponseEntity<Map<String, String>> handleOnboardingClassification(OnboardingClassificationException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(SpecialElixirValidationException.class)
    public ResponseEntity<Map<String, String>> handleSpecialElixirValidation(SpecialElixirValidationException e) {
        return badRequest(e.getMessage());
    }

    @ExceptionHandler(AlchemyNameGenerationException.class)
    public ResponseEntity<Map<String, String>> handleAlchemyNameGeneration(AlchemyNameGenerationException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(SupplementVerificationException.class)
    public ResponseEntity<Map<String, String>> handleSupplementVerification(SupplementVerificationException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return badRequest("요청 값이 유효하지 않습니다");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return badRequest("요청 본문을 읽을 수 없습니다");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        return badRequest("필수 파라미터 '" + e.getParameterName() + "'이(가) 누락되었습니다");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        return badRequest("파라미터 '" + e.getName() + "'의 값이 올바르지 않습니다");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, String>> handleMissingServletRequestPart(MissingServletRequestPartException e) {
        return badRequest("필수 파라미터 '" + e.getRequestPartName() + "'이(가) 누락되었습니다");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return badRequest("이미지 파일 크기가 너무 큽니다(최대 10MB)");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        log.error("예상하지 못한 오류가 발생했습니다", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "서버 오류가 발생했습니다"));
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
