package fr.uspn.cellula.rule.parser;

/**
 * A lexical unit of the rule language.
 *
 * @param type     the kind of token
 * @param text     the characters of the token, as written in the source
 * @param position offset of the first character of the token in the source
 */
record Token(Type type, String text, int position) {

    /** The kinds of tokens. */
    enum Type {
        /** An integer literal, possibly negative. */
        NUMBER("a number"),
        /**
         * An operator keyword or a neighborhood name, e.g. {@code SI} or {@code G8*}.
         */
        NAME("a name"),
        /** An opening parenthesis. */
        LEFT_PAREN("'('"),
        /** A closing parenthesis. */
        RIGHT_PAREN("')'"),
        /** An argument separator. */
        COMMA("','"),
        /** The end of the source. */
        END("the end of the rule");

        private final String description;

        Type(String description) {
            this.description = description;
        }

        /** Returns a phrase naming this kind of token in error messages. */
        String description() {
            return description;
        }
    }

    /** Returns a phrase naming this very token in error messages. */
    String describe() {
        return switch (type) {
            case NUMBER, NAME -> "'" + text + "'";
            default -> type.description();
        };
    }
}
