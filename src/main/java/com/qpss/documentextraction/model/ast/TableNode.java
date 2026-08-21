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
public class TableNode implements AstNode {
    @Builder.Default
    private List<TableRowNode> rows = new ArrayList<>();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TableRowNode {
        @Builder.Default
        private List<TableCellNode> cells = new ArrayList<>();
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TableCellNode {
        @Builder.Default
        private List<AstNode> content = new ArrayList<>();
    }
}
