package com.opencode.minecraft.gui.markdown;

/**
 * Represents a styled segment of text with color and formatting
 */
public class TextSegment {
    private final String text;
    private final int color;
    private final boolean bold;
    private final boolean italic;
    private final boolean code;
    private final boolean header;

    public TextSegment(String text, int color) {
        this(text, color, false, false, false, false);
    }

    public TextSegment(String text, int color, boolean bold, boolean italic, boolean code, boolean header) {
        this.text = text;
        this.color = color;
        this.bold = bold;
        this.italic = italic;
        this.code = code;
        this.header = header;
    }

    public String getText() {
        return text;
    }

    public int getColor() {
        return color;
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public boolean isCode() {
        return code;
    }

    public boolean isHeader() {
        return header;
    }

    public int getEffectiveColor() {
        // Apply brightness adjustments for styling
        if (bold || header) {
            // Make bold/headers brighter
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            // Increase brightness by 20%
            r = Math.min(255, (int)(r * 1.2));
            g = Math.min(255, (int)(g * 1.2));
            b = Math.min(255, (int)(b * 1.2));

            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        return color;
    }
}
