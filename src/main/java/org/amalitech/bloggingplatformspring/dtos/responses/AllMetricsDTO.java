package org.amalitech.bloggingplatformspring.dtos.responses;

import java.time.LocalDateTime;
import java.util.List;

public record AllMetricsDTO(
        int totalMethods,
        LocalDateTime timestamp,
        List<MethodMetricsDTO> metrics) {
}