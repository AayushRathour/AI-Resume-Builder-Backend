package com.resumeai.template.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.resumeai.template.exception.TemplateValidationException;

/** Provides supporting template validation operations for workflow execution. */

@Service
public class TemplateValidationService {

    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\"'\\s<>]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DUPLICATE_PLACEHOLDER = Pattern.compile("(\\{\\{\\s*[a-zA-Z_]+\\s*\\}\\})\\s*\\1");

    private static final String[] LINK_LABELS = {
            "LinkedIn",
            "GitHub",
            "Portfolio",
            "Website",
            "Behance",
            "LeetCode",
            "CodeChef",
            "Codeforces"
    };

    public String sanitizeHtml(String html) {
        if (html == null) {
            return "";
        }

        String cleaned = html.replaceAll("(?is)<script[^>]*>.*?</script>", "");
        boolean hasHtmlTag = cleaned.toLowerCase(Locale.ROOT).contains("<html");

        Document doc = Jsoup.parse(cleaned);
        doc.select("script").remove();

        sanitizeAnchors(doc.select("a"));
        removeDuplicateLinkLabelArtifacts(doc);

        String output = hasHtmlTag ? doc.outerHtml() : doc.body().html();
        return DUPLICATE_PLACEHOLDER.matcher(output).replaceAll("$1");
    }

    public void validateHtml(String html) {
        List<String> errors = new ArrayList<>();

        if (html == null || html.isBlank()) {
            errors.add("Template HTML is required");
        } else if (!TAG_PATTERN.matcher(html).find()) {
            errors.add("Template must contain valid HTML tags");
        }

        Document doc = Jsoup.parse(html == null ? "" : html);
        Elements anchors = doc.select("a");
        for (Element anchor : anchors) {
            String href = anchor.attr("href");
            if (href != null && (href.contains("<") || href.contains(">") || href.contains("href="))) {
                errors.add("Template contains malformed anchor href attributes");
                break;
            }
        }

        for (String label : LINK_LABELS) {
            Pattern orphanPattern = Pattern.compile("(?i)</a>\\s*\\\"\\s*>\\s*" + Pattern.quote(label));
            if (orphanPattern.matcher(html == null ? "" : html).find()) {
                errors.add("Template contains orphan link label: " + label);
            }
        }

        if (!errors.isEmpty()) {
            throw new TemplateValidationException(String.join(" | ", errors));
        }
    }

    private void sanitizeAnchors(Elements anchors) {
        for (Element anchor : anchors) {
            String href = anchor.attr("href");
            if (href == null || href.isBlank()) {
                continue;
            }

            if (href.contains("{{") && href.contains("}}")) {
                continue;
            }

            if (href.contains("<") || href.contains(">") || href.contains("href=")) {
                Matcher matcher = URL_PATTERN.matcher(href);
                if (matcher.find()) {
                    anchor.attr("href", matcher.group());
                } else {
                    anchor.removeAttr("href");
                }
            }
        }
    }

    private void removeDuplicateLinkLabelArtifacts(Document doc) {
        List<TextNode> textNodes = doc.textNodes();
        for (TextNode node : textNodes) {
            String text = node.text().trim();
            if (!text.startsWith("\">") || text.length() < 3) {
                continue;
            }
            String label = text.substring(2).trim();
            if (!isKnownLabel(label)) {
                continue;
            }
            Node prev = node.previousSibling();
            if (prev instanceof Element prevEl && "a".equalsIgnoreCase(prevEl.tagName())) {
                if (label.equalsIgnoreCase(prevEl.text().trim())) {
                    node.remove();
                }
            }
        }
    }

    private boolean isKnownLabel(String label) {
        for (String known : LINK_LABELS) {
            if (known.equalsIgnoreCase(label)) {
                return true;
            }
        }
        return false;
    }
}



