package fr.uspn.cellula.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import fr.uspn.cellula.config.InitialState.RandomFill;
import fr.uspn.cellula.grid.Coordinates;

/**
 * Writes automaton definitions as XML files, in the format read by
 * {@link XmlDefinitionReader}.
 *
 * <p>
 * Reading back a written definition yields an equal definition. The writer is
 * what lets the
 * rule explorer hand its discoveries over as ordinary input files.
 */
public final class XmlDefinitionWriter {

    private XmlDefinitionWriter() {
    }

    /**
     * Writes a definition to a file, creating the parent directories if needed.
     *
     * @param definition the definition to save
     * @param file       the destination; overwritten if it exists
     * @throws IOException if the file cannot be written
     */
    public static void write(AutomatonDefinition definition, Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, toXml(definition), StandardCharsets.UTF_8);
    }

    /**
     * Returns the XML document describing a definition.
     *
     * @param definition the definition to serialize
     */
    public static String toXml(AutomatonDefinition definition) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<automaton name=\"").append(escape(definition.name())).append("\">\n");
        if (!definition.description().isEmpty()) {
            element(xml, "description", definition.description());
        }
        element(xml, "dimension", Integer.toString(definition.dimension()));
        element(xml, "size", sizes(definition));
        element(xml, "boundary", definition.boundary().label());
        if (definition.dimension() >= 3) {
            element(xml, "slice", definition.slice().toString());
        }
        if (!definition.neighborhoods().isEmpty()) {
            xml.append("  <neighborhoods>\n");
            definition.neighborhoods().forEach((name, offsets) -> xml.append("    <neighborhood name=\"")
                    .append(escape(name))
                    .append("\">")
                    .append(set(offsets))
                    .append("</neighborhood>\n"));
            xml.append("  </neighborhoods>\n");
        }
        xml.append("  <rule>\n")
                .append(escape(definition.rule().strip()).indent(4))
                .append("  </rule>\n");

        xml.append("  <initial");
        if (definition.initialState() instanceof RandomFill random && random.seed().isPresent()) {
            xml.append(" seed=\"").append(random.seed().getAsLong()).append('"');
        }
        xml.append('>').append(escape(definition.initialState().describe())).append("</initial>\n");

        if (!definition.colors().isEmpty()) {
            xml.append("  <colors>\n");
            definition.colors().forEach((state, rgb) -> xml.append(String.format(
                    Locale.ROOT, "    <state value=\"%d\" color=\"#%06X\"/>%n", state, rgb)));
            xml.append("  </colors>\n");
        }
        xml.append("</automaton>\n");
        return xml.toString().replace("\r\n", "\n");
    }

    private static void element(StringBuilder xml, String name, String content) {
        xml.append("  <").append(name).append('>')
                .append(escape(content))
                .append("</").append(name).append(">\n");
    }

    private static String sizes(AutomatonDefinition definition) {
        StringJoiner joiner = new StringJoiner(", ");
        for (int size : definition.shape().toArray()) {
            joiner.add(Integer.toString(size));
        }
        return joiner.toString();
    }

    private static String set(List<Coordinates> offsets) {
        return offsets.stream().map(Coordinates::toString).collect(Collectors.joining(", ", "{", "}"));
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
