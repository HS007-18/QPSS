package com.qpss.documentextraction.model.ast;

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
public class ParagraphNode implements AstNode {
    private String alignment; // "LEFT", "CENTER", "RIGHT", "BOTH"
    private boolean isListItem;
    private String listSymbol; // "•", "1.", etc.
    @Builder.Default
    private List<AstNode> children = new ArrayList<>();
}
