package com.qpss.dto;

import lombok.Data;

@Data
public class FixQuestionCommand {
    private String file;
    private Integer serialNo;
    private Integer unit;
    private String co;
    private Integer marks;
    private Integer t;
    private String rbt;
    private String questionContent;
}