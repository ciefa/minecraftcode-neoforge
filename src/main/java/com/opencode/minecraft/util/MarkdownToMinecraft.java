package com.opencode.minecraft.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts markdown formatting to Minecraft text formatting.
 */
public class MarkdownToMinecraft {

    // Patterns for markdown elements
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.+?)\\*");
    private static final Pattern CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,3})\\s+(.+)$");

    /**
     * Converts a markdown string to Minecraft Component
     */
    public static Component convert(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return Component.empty();
        }

        // Check for headers first
        Matcher headerMatcher = HEADER_PATTERN.matcher(markdown);
        if (headerMatcher.matches()) {
            String headerText = headerMatcher.group(2);
            return Component.literal(headerText)
                    .withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE);
        }

        // Process inline formatting
        return processInlineFormatting(markdown);
    }

    private static Component processInlineFormatting(String text) {
        MutableComponent result = Component.empty();

        // Track position in the string
        int pos = 0;

        while (pos < text.length()) {
            boolean matched = false;

            // Try to match code first (highest priority)
            if (text.substring(pos).startsWith("`")) {
                Matcher codeMatcher = CODE_PATTERN.matcher(text.substring(pos));
                if (codeMatcher.find() && codeMatcher.start() == 0) {
                    result.append(Component.literal(codeMatcher.group(1))
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                    pos += codeMatcher.end();
                    matched = true;
                }
            }

            // Try bold (**text**)
            if (!matched && text.substring(pos).startsWith("**")) {
                Matcher boldMatcher = BOLD_PATTERN.matcher(text.substring(pos));
                if (boldMatcher.find() && boldMatcher.start() == 0) {
                    result.append(Component.literal(boldMatcher.group(1))
                            .withStyle(ChatFormatting.BOLD));
                    pos += boldMatcher.end();
                    matched = true;
                }
            }

            // Try italic (*text*)
            if (!matched && text.substring(pos).startsWith("*")) {
                Matcher italicMatcher = ITALIC_PATTERN.matcher(text.substring(pos));
                if (italicMatcher.find() && italicMatcher.start() == 0) {
                    result.append(Component.literal(italicMatcher.group(1))
                            .withStyle(ChatFormatting.ITALIC));
                    pos += italicMatcher.end();
                    matched = true;
                }
            }

            // No match - add plain character
            if (!matched) {
                // Find the next potential formatting character
                int nextSpecial = findNextSpecial(text, pos + 1);
                if (nextSpecial == -1) {
                    nextSpecial = text.length();
                }

                result.append(Component.literal(text.substring(pos, nextSpecial)));
                pos = nextSpecial;
            }
        }

        return result;
    }

    private static int findNextSpecial(String text, int start) {
        int minPos = -1;

        int bold = text.indexOf("**", start);
        int italic = text.indexOf("*", start);
        int code = text.indexOf("`", start);

        if (bold != -1 && (minPos == -1 || bold < minPos)) minPos = bold;
        if (italic != -1 && (minPos == -1 || italic < minPos)) minPos = italic;
        if (code != -1 && (minPos == -1 || code < minPos)) minPos = code;

        return minPos;
    }

    /**
     * Strips all markdown formatting from text
     */
    public static String stripMarkdown(String markdown) {
        if (markdown == null) return "";

        String result = markdown;
        result = BOLD_PATTERN.matcher(result).replaceAll("$1");
        result = ITALIC_PATTERN.matcher(result).replaceAll("$1");
        result = CODE_PATTERN.matcher(result).replaceAll("$1");
        result = HEADER_PATTERN.matcher(result).replaceAll("$2");

        return result;
    }
}
