package org.amalitech.bloggingplatformspring.dtos;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class SessionInfo {
    String email;
    String accessToken;
    String refreshToken;
    long loginTimestamp;
    long lastActivityTimestamp;
}