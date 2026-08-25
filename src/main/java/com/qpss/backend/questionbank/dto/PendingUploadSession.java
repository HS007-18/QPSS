package com.qpss.backend.questionbank.dto;
import com.qpss.documentextraction.model.QuestionParseResult;
import lombok.Data;
import java.io.Serial;
import java.util.List;
@Data
public class PendingUploadSession {
    private Long subjectId;
    private Long sessionId;
    private List<FileImportHolder> files;

    @Data
    public static class FileImportHolder implements java.io.Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private int index;
        private String originalName;
        private String checksum;
        private QuestionParseResult parseResult;
        private String tempFilePath;
        private String contentType;
        private long size;
    }
}