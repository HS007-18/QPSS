package com.qpss.document.parser;

import com.qpss.document.model.ast.AstNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentExtractionResult {
    private List<AstNode> astNodes;
    private String htmlFallback;
}
