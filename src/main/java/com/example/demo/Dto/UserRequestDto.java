package com.example.demo.Dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserRequestDto {

    @Getter
    @NoArgsConstructor
    public static class SignUp {
        private String email;
        private String password;
        private String selectedCategory;
    }

    @Getter
    @NoArgsConstructor
    public static class Login {
        private String email;
        private String password;
    }
}