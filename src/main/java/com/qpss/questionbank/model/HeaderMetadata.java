package com.qpss.questionbank.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeaderMetadata {

    private String institutionName;
    private String tagline;
    private String examTitle;
    private String semester;
    private String department;
    private String subjectCodeTitle;
    private String commonTo;
    private String notes;
    private String regulation;

    @Builder.Default
    private List<CourseOutcome> courseOutcomes = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseOutcome {
        private String code;
        private String description;
    }
}
