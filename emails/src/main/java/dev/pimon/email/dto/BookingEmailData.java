package dev.pimon.email.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingEmailData {
    private String clientName;
    private String clientEmail;
    private String serviceName;
    private String date;        // "15 de junio de 2026"
    private String time;        // "10:00 h"
    private String notes;
    private String status;      // "confirmed", "cancelled", "completed"
    private String studioName;
    private String studioEmail;
    private String studioPhone;
}
