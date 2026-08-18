package com.qpss.questionbank.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class QuestionContentSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addAttributes("img", "src", "alt", "width", "height")
            .addAttributes(":all", "class", "style")
            .addProtocols("img", "src", "data");

    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        Document doc = Jsoup.parseBodyFragment(html);
        doc.select("*").forEach(el -> {
            el.attributes().asList().stream()
                    .filter(a -> a.getKey().toLowerCase().startsWith("on"))
                    .forEach(a -> el.removeAttr(a.getKey()));
            String src = el.attr("src");
            if (!src.isEmpty() && !src.startsWith("data:image/")) {
                el.removeAttr("src");
            }
        });
        return Jsoup.clean(doc.body().html(), SAFELIST);
    }
}