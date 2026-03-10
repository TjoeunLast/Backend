package com.example.project.member.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminUserSuspendRequest {

    @NotNull
    @Min(1)
    private Integer days;
}
