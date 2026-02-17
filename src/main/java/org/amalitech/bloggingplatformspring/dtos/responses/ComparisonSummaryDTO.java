package org.amalitech.bloggingplatformspring.dtos.responses;

/**
 * DTO for overall comparison summary.
 */
public record ComparisonSummaryDTO(
    int methodsCompared,
    int methodsImproved,
    int methodsDegraded,
    int methodsUnchanged,
    String overallAvgImprovementPercent,
    String bestImprovedMethod,
    String bestImprovementPercent,
    String worstMethod,
    String worstChangePercent) {
}
