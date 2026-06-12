package dev.pimon.email.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContactFormData {
    private String fromName;
    private String fromEmail;
    private String phone;
    private String subject;
    private String message;
}
