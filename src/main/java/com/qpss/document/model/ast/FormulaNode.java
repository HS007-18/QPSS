package com.qpss.document.model.ast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormulaNode implements AstNode {
    private String rawOmml; // The exact OMML XML string
    private String htmlFallback; // An HTML preview representation for the UI
}
