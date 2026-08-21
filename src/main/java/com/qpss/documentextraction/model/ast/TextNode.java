package com.qpss.documentextraction.model.ast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TextNode implements AstNode {
    private String text;
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean subscript;
    private boolean superscript;
}
