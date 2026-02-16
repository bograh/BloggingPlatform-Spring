package org.amalitech.bloggingplatformspring.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfo {
    private String email;
    private String accessToken;
    private String refreshToken;
    private long loginTimestamp;
    private long lastActivityTimestamp;
}