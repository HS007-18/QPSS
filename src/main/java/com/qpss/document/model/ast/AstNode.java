package com.qpss.document.model.ast;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ParagraphNode.class, name = "paragraph"),
    @JsonSubTypes.Type(value = TableNode.class, name = "table"),
    @JsonSubTypes.Type(value = TextNode.class, name = "text"),
    @JsonSubTypes.Type(value = FormulaNode.class, name = "formula"),
    @JsonSubTypes.Type(value = ImageNode.class, name = "image")
})
public interface AstNode {
}
