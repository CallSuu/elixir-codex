package com.example.demo.Dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class HealthSyncRequestDto {
    private Integer steps;
    private Integer sleepHours;
}
