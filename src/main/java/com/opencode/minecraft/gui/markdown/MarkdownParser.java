package com.opencode.minecraft.gui.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses markdown text into formatted lines with styled segments
 */
public class MarkdownParser {
    // Expanded autumn theme color palette - vibrant and varied
    private static final int COLOR_TEXT = 0xFFfff8dc;      // Warm white (cornsilk)
    private static final int COLOR_BOLD = 0xFFb7410e;      // Rust red for bold/headers
    private static final int COLOR_CODE = 0xFFffd700;      // Golden yellow for inline code
    private static final int COLOR_CODE_BLOCK = 0xFFd2691e; // Copper for code blocks
    private static final int COLOR_LINK = 0xFF800020;      // Burgundy for links

    // Regex patterns for inline markdown
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*|__(.+?)__");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.+?)\\*|_(.+?)_");
    private static final Pattern CODE_PATTERN = Pattern.compile("`([^`]+?)`");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+?)\\]\\(([^\\)]+?)\\)");

    /**
     * Parse markdown text into formatted lines
     */
    public static List<FormattedLine> parse(String markdownText, int baseColor) {
        List<FormattedLine> lines = new ArrayList<>();
        String[] rawLines = markdownText.split("\n");

        boolean inCodeBlock = false;
        String codeBlockLang = "";

        for (String line : rawLines) {
            // Check for code block start/end
            if (line.trim().startsWith("```")) {
                if (!inCodeBlock) {
                    // Starting code block
                    inCodeBlock = true;
                    codeBlockLang = line.trim().substring(3).trim();
                    // Add a separator line
                    FormattedLine separator = new FormattedLine();
                    separator.addSegment("┌─[ " + (codeBlockLang.isEmpty() ? "code" : codeBlockLang) + " ]", COLOR_CODE);
                    lines.add(separator);
                } else {
                    // Ending code block
                    inCodeBlock = false;
                    FormattedLine separator = new FormattedLine();
                    separator.addSegment("└" + "─".repeat(20), COLOR_CODE);
                    lines.add(separator);
                }
                continue;
            }

            if (inCodeBlock) {
                // Code block line - preserve formatting, indent, use code color
                FormattedLine codeLine = new FormattedLine(new ArrayList<>(), true, 1);
                codeLine.addSegment(line, COLOR_CODE_BLOCK);
                lines.add(codeLine);
            } else {
                // Regular line - parse inline markdown
                FormattedLine formattedLine = parseInlineMarkdown(line, baseColor);
                lines.add(formattedLine);
            }
        }

        return lines;
    }

    /**
     * Parse inline markdown within a single line
     */
    private static FormattedLine parseInlineMarkdown(String line, int baseColor) {
        FormattedLine formattedLine = new FormattedLine();

        // Check for header
        int headerLevel = 0;
        String trimmed = line.trim();
        while (trimmed.startsWith("#")) {
            headerLevel++;
            trimmed = trimmed.substring(1);
        }
        if (headerLevel > 0 && trimmed.startsWith(" ")) {
            // This is a header
            trimmed = trimmed.trim();
            formattedLine.addSegment(trimmed, COLOR_BOLD);
            return formattedLine;
        }

        // Check for list items
        int indentLevel = 0;
        if (line.trim().startsWith("- ") || line.trim().startsWith("* ")) {
            formattedLine = new FormattedLine(new ArrayList<>(), false, 1);
            line = "• " + line.trim().substring(2);
        } else if (line.trim().matches("^\\d+\\.\\s+.*")) {
            formattedLine = new FormattedLine(new ArrayList<>(), false, 1);
        }

        // Parse inline formatting (bold, italic, code, links)
        parseInlineFormatting(line, baseColor, formattedLine);

        return formattedLine;
    }

    /**
     * Parse inline formatting like **bold**, *italic*, `code`, [links]
     */
    private static void parseInlineFormatting(String text, int baseColor, FormattedLine line) {
        List<FormatSpan> spans = new ArrayList<>();

        // Find all formatting spans
        Matcher boldMatcher = BOLD_PATTERN.matcher(text);
        while (boldMatcher.find()) {
            String content = boldMatcher.group(1) != null ? boldMatcher.group(1) : boldMatcher.group(2);
            spans.add(new FormatSpan(boldMatcher.start(), boldMatcher.end(), content, FormatType.BOLD));
        }

        Matcher codeMatcher = CODE_PATTERN.matcher(text);
        while (codeMatcher.find()) {
            spans.add(new FormatSpan(codeMatcher.start(), codeMatcher.end(), codeMatcher.group(1), FormatType.CODE));
        }

        Matcher linkMatcher = LINK_PATTERN.matcher(text);
        while (linkMatcher.find()) {
            spans.add(new FormatSpan(linkMatcher.start(), linkMatcher.end(), linkMatcher.group(1), FormatType.LINK));
        }

        // Sort spans by start position
        spans.sort((a, b) -> Integer.compare(a.start, b.start));

        // Build segments
        int currentPos = 0;
        for (FormatSpan span : spans) {
            // Add text before this span
            if (span.start > currentPos) {
                String before = text.substring(currentPos, span.start);
                if (!before.isEmpty()) {
                    line.addSegment(before, baseColor);
                }
            }

            // Add formatted span
            int color = baseColor;
            boolean isBold = false;
            boolean isCode = false;

            switch (span.type) {
                case BOLD:
                    color = COLOR_BOLD;
                    isBold = true;
                    break;
                case CODE:
                    color = COLOR_CODE;
                    isCode = true;
                    break;
                case LINK:
                    color = COLOR_LINK;
                    break;
            }

            line.addSegment(new TextSegment(span.content, color, isBold, false, isCode, false));
            currentPos = span.end;
        }

        // Add remaining text
        if (currentPos < text.length()) {
            String remaining = text.substring(currentPos);
            if (!remaining.isEmpty()) {
                line.addSegment(remaining, baseColor);
            }
        }

        // If no formatting found, add the whole line
        if (line.getSegments().isEmpty()) {
            line.addSegment(text, baseColor);
        }
    }

    /**
     * Helper class to track format spans
     */
    private static class FormatSpan {
        int start;
        int end;
        String content;
        FormatType type;

        FormatSpan(int start, int end, String content, FormatType type) {
            this.start = start;
            this.end = end;
            this.content = content;
            this.type = type;
        }
    }

    private enum FormatType {
        BOLD, ITALIC, CODE, LINK
    }
}
