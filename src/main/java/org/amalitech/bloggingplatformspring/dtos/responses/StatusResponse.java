package org.amalitech.bloggingplatformspring.dtos.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusResponse {
    private String status;
    private String message;

    public StatusResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public static StatusResponse success(String message) {
        return new StatusResponse("success", message);
    }
}