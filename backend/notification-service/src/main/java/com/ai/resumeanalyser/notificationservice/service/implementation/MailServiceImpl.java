package com.ai.resumeanalyser.notificationservice.service.implementation;

import brevo.ApiClient;
import brevo.ApiException;
import brevo.Configuration;
import brevoApi.TransactionalEmailsApi;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailSender;
import brevoModel.SendSmtpEmailTo;
import com.ai.resumeanalyser.notificationservice.service.MailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Collections;


@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    @Value("${apiKey}")
    private String apiKey;

    private final TemplateEngine templateEngine;

    private ApiClient buildClient() {
        ApiClient apiClient = Configuration.getDefaultApiClient();
        apiClient.setApiKey(apiKey);
        apiClient.setConnectTimeout(8000);
        apiClient.setReadTimeout(10000);
        apiClient.setWriteTimeout(10000);
        return apiClient;
    }

    @Override
    public void sentVerifyOtp(String username, String email, String otp) throws MessagingException {
        String toEmail = email.substring(0, 1) + "*********" + email.substring(email.indexOf("@"));

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("email", toEmail);
        context.setVariable("otp", otp);

        String message = templateEngine.process("verify-otp", context);
        sendEmail(username, email, "Email verification OTP", message);
    }

    @Override
    public void sentResetOtp(String username, String email, String otp) throws MessagingException {
        String toEmail = email.substring(0, 1) + "*********" + email.substring(email.indexOf("@"));

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("email", toEmail);
        context.setVariable("otp", otp);

        String message = templateEngine.process("reset-otp", context);
        sendEmail(username, email, "Reset Password OTP", message);
    }

    private void sendEmail(String username, String email, String subject, String htmlContent) {
        ApiClient apiClient = buildClient();
        TransactionalEmailsApi transactionalEmailsApi = new TransactionalEmailsApi(apiClient);

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.setSender(new SendSmtpEmailSender().name("Resume Analyser").email("rohithgxwda001@gmail.com"));
        sendSmtpEmail.setTo(Collections.singletonList(new SendSmtpEmailTo().name(username).email(email)));
        sendSmtpEmail.setSubject(subject);
        sendSmtpEmail.setHtmlContent(htmlContent);

        try {
            transactionalEmailsApi.sendTransacEmail(sendSmtpEmail);
        } catch (ApiException e) {
            System.err.println("========== BREVO ERROR ==========");
            System.err.println("Status Code : " + e.getCode());
            System.err.println("Message     : " + e.getMessage());
            System.err.println("Response    : " + e.getResponseBody());
            System.err.println("Headers     : " + e.getResponseHeaders());
            System.err.println("=================================");

            throw new RuntimeException(
                    "Brevo send failed: HTTP " + e.getCode()
                            + " - " + e.getResponseBody(),
                    e
            );

        } catch (Exception e) {
            System.err.println("========== MAIL ERROR ==========");
            System.err.println("Message : " + e.getMessage());
            e.printStackTrace();
            System.err.println("================================");
            throw new RuntimeException(
                    "Unexpected mail send error: " + e.getMessage(),
                    e
            );
        }
    }
}
