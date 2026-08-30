package com.ai.resumeanalyser.authservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Entity
@Table(name = "otp_verify")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OtpVerify {
    @Id
    private String email;
    private String verifyOtp;
    private Date verifyExpiration;
}
