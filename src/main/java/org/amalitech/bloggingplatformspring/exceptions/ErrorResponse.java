package org.amalitech.bloggingplatformspring.exceptions;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class ErrorResponse {
    private String errorStatus;
    private String errorMessage;
    private int errorCode;
    private String timestamp;

    public ErrorResponse(String errorStatus, String errorMessage, int errorCode) {
        this.errorStatus = errorStatus;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }
}