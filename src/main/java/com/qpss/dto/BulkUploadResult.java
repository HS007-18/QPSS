package com.qpss.dto;
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
public class BulkUploadResult {

    @Builder.Default
    private int totalFiles = 0;

    @Builder.Default
    private int processedFiles = 0;

    @Builder.Default
    private int totalQuestions = 0;

    @Builder.Default
    private int subjectsCreated = 0;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<SubjectSummary> subjects = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubjectSummary {
        private Long subjectId;
        private String code;
        private String name;
        private int filesProcessed;
        private int questionsImported;
    }
}
