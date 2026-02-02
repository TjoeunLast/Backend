package com.example.project.member.domain;

import java.time.LocalTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name = "user_settings")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettings {
    @Id
    private Long userId;

    @OneToOne @MapsId
    @JoinColumn(name = "user_id")
    private Users user;

    private boolean pushEnabled;
    private boolean expiryAlertEnabled;
    private int expiryThresholdDays;
    private LocalTime quietStart;
    private LocalTime quietEnd;
    private boolean groupbuyAlertEnabled;

}


