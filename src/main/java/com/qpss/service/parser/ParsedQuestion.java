package com.qpss.service.parser;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParsedQuestion {
    private Integer serialNo;
    private String questionContent;
    private String rawOoxml;
    private Integer marks;
    private String co;
    private Integer t;
    private Integer unit;
}
