package com.qpss.frontend.dto;
import com.qpss.documentextraction.model.QuestionParseResult;
import lombok.Data;
import java.util.List;
@Data
public class PendingUploadSession {
    private Long subjectId;
    private Long sessionId;
    private List<FileImportHolder> files;

    @Data
    public static class FileImportHolder {
        private int index;
        private String originalName;
        private String checksum;
        private QuestionParseResult parseResult;
        private byte[] fileBytes;
        private String contentType;
        private long size;
    }
}