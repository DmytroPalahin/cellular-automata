package fr.uspn.cellula.rule.parser;

/**
 * Thrown when the text of a rule is not a valid expression of the rule language.
 *
 * <p>The exception remembers where the problem was found, and its message quotes the offending
 * line with a caret under the culprit:
 *
 * <pre>
 * SI expects 3 arguments but got 2 (line 1, column 1)
 *     SI(EQ(COMPTER(G0), 1), 0)
 *     ^
 * </pre>
 */
public final class RuleSyntaxException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final String reason;
    private final String source;
    private final int position;

    /**
     * Creates the exception.
     *
     * @param reason what is wrong, without location
     * @param source the full text of the rule
     * @param position offset in {@code source} of the first offending character
     */
    public RuleSyntaxException(String reason, String source, int position) {
        super(render(reason, source, position));
        this.reason = reason;
        this.source = source;
        this.position = position;
    }

    /** Returns what is wrong, without the location and the source excerpt. */
    public String reason() {
        return reason;
    }

    /** Returns the full text of the rule. */
    public String source() {
        return source;
    }

    /** Returns the offset of the first offending character in {@link #source()}. */
    public int position() {
        return position;
    }

    private static String render(String reason, String source, int position) {
        int offset = Math.max(0, Math.min(position, source.length()));
        int lineStart = source.lastIndexOf('\n', offset - 1) + 1;
        int lineEnd = source.indexOf('\n', offset);
        if (lineEnd < 0) {
            lineEnd = source.length();
        }
        int lineNumber = 1 + (int) source.substring(0, lineStart).chars().filter(c -> c == '\n').count();
        int column = offset - lineStart;

        // Tabs would misalign the caret, so they are shown as single spaces.
        String line = source.substring(lineStart, lineEnd).replace('\t', ' ').replace('\r', ' ');
        return reason + " (line " + lineNumber + ", column " + (column + 1) + ")\n"
                + "    " + line + "\n"
                + "    " + " ".repeat(column) + "^";
    }
}
