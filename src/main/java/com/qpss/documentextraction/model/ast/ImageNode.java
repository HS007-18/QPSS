package com.qpss.documentextraction.model.ast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageNode implements AstNode {
    private String base64Data;
    private String mimeType;
    private int width;
    private int height;
}
