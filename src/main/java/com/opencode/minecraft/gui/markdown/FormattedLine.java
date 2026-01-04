package com.opencode.minecraft.gui.markdown;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a line with multiple styled text segments
 */
public class FormattedLine {
    private final List<TextSegment> segments;
    private final boolean codeBlock;
    private final int indentLevel;

    public FormattedLine() {
        this(new ArrayList<>(), false, 0);
    }

    public FormattedLine(List<TextSegment> segments, boolean codeBlock, int indentLevel) {
        this.segments = segments;
        this.codeBlock = codeBlock;
        this.indentLevel = indentLevel;
    }

    public List<TextSegment> getSegments() {
        return segments;
    }

    public void addSegment(TextSegment segment) {
        segments.add(segment);
    }

    public void addSegment(String text, int color) {
        segments.add(new TextSegment(text, color));
    }

    public boolean isCodeBlock() {
        return codeBlock;
    }

    public int getIndentLevel() {
        return indentLevel;
    }

    public boolean isEmpty() {
        return segments.isEmpty() ||
               (segments.size() == 1 && segments.get(0).getText().trim().isEmpty());
    }

    /**
     * Gets the plain text content (for width calculations)
     */
    public String getPlainText() {
        StringBuilder sb = new StringBuilder();
        // Add indent
        for (int i = 0; i < indentLevel; i++) {
            sb.append("  ");
        }
        // Add text
        for (TextSegment segment : segments) {
            sb.append(segment.getText());
        }
        return sb.toString();
    }
}
