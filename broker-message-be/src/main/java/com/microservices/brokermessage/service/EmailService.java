package com.microservices.brokermessage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${notification.email.from}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        logger.info("Email sent to={} subject={}", to, subject);
    }

    public void sendFailureEmail(String to, String jobId, String entityId, String action, String errorMessage) {
        String subject = "[RETRY FAILED] " + action + " | Job: " + jobId;
        String body = String.format(
            "Retry job has permanently failed after maximum attempts.%n%n" +
            "Job ID  : %s%n" +
            "Entity  : %s%n" +
            "Action  : %s%n" +
            "Error   : %s",
            jobId, entityId, action, errorMessage
        );
        sendEmail(to, subject, body);
    }
}
