package com.qpss.service;

import com.qpss.model.Question;
import lombok.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.zwobble.mammoth.DocumentConverter;
import org.zwobble.mammoth.Result;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionParserService {

    private static final Pattern CO_PATTERN = Pattern.compile("CO(\\d+)");

    @Data @AllArgsConstructor
    public static class ParsedQuestion {
        private int serialNo;
        private String questionContent;
        private int marks;
        private String co;
        private int unit;
    }

    public List<ParsedQuestion> parseDocx(MultipartFile file) throws IOException {
        DocumentConverter converter = new DocumentConverter();
        Result<String> result = converter.convertToHtml(file.getInputStream());
        String html = result.getValue();
        return parseHtml(html);
    }

    List<ParsedQuestion> parseHtml(String html) {
        List<ParsedQuestion> results = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        
        Elements tables = doc.select("table");
        for (Element table : tables) {
            Elements rows = table.select("tr");
            for (Element row : rows) {
                Elements cells = row.select("td, th");
                if (cells.size() >= 4) {
                    String snoText = cells.get(0).text().trim();
                    String questionHtml = cells.get(1).html(); // keep raw HTML for images
                    String marksText = cells.get(2).text().trim();
                    String coText = cells.get(3).text().trim();
                    
                    if (snoText.equalsIgnoreCase("S.No") || snoText.equalsIgnoreCase("S.No.")) {
                        continue; // header row
                    }
                    
                    try {
                        int serialNo = Integer.parseInt(snoText);
                        int marks = Integer.parseInt(marksText);
                        int unit = deriveUnit(coText);
                        
                        results.add(new ParsedQuestion(serialNo, questionHtml, marks, coText, unit));
                    } catch (NumberFormatException e) {
                        // skip row if it's not a valid question row
                    }
                }
            }
        }
        
        return results;
    }

    private int deriveUnit(String co) {
        Matcher m = CO_PATTERN.matcher(co);
        if (m.matches()) {
            return Integer.parseInt(m.group(1));
        }
        return 1;
    }

    public List<Question> toQuestions(List<ParsedQuestion> parsed, Long subjectId,
                                      Long sessionId, String sourceFileName) {
        List<Question> questions = new ArrayList<>();
        for (ParsedQuestion p : parsed) {
            questions.add(Question.builder()
                    .subjectId(subjectId)
                    .sessionId(sessionId)
                    .unit(p.getUnit())
                    .co(p.getCo())
                    .marks(p.getMarks())
                    .serialNo(p.getSerialNo())
                    .questionContent(p.getQuestionContent()) // contains HTML + Base64
                    .sourceFileName(sourceFileName)
                    .build());
        }
        return questions;
    }
}
