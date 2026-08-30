package com.qpss.util;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
@Component
public class QuestionContentSanitizer {
    private static final int MAX_INPUT_LENGTH = 5_000_000;
    private static final int MAX_OUTPUT_LENGTH = 5_500_000;
    private static final Safelist SAFELIST = Safelist.relaxed()
            .addAttributes("img", "src", "alt", "width", "height")
            .addAttributes(":all", "class")
            .addProtocols("img", "src", "data");
    public String sanitize(String html) {
        if (html == null || html.isBlank()) return html;
        if (html.length() > MAX_INPUT_LENGTH) {
            throw new IllegalArgumentException("Input content exceeds maximum allowed length of " + MAX_INPUT_LENGTH);
        }
        Document doc = Jsoup.parseBodyFragment(html);
        doc.select("*").forEach(el -> {
            el.attributes().asList().stream()
                    .filter(a -> a.getKey().toLowerCase().startsWith("on"))
                    .forEach(a -> el.removeAttr(a.getKey()));
            String src = el.attr("src");
            if (!src.isEmpty() && !src.startsWith("data:image/")) el.removeAttr("src");
        });
        String cleaned = Jsoup.clean(doc.body().html(), SAFELIST);
        if (cleaned.length() > MAX_OUTPUT_LENGTH) {
            throw new IllegalArgumentException("Sanitized content exceeds maximum allowed length of " + MAX_OUTPUT_LENGTH);
        }
        return cleaned;
    }
}