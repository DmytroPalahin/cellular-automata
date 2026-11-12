package fr.uspn.cellula.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import fr.uspn.cellula.grid.BoundaryCondition;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Shape;
import fr.uspn.cellula.grid.Slice;

/**
 * Reads automaton definitions from XML files.
 *
 * <p>
 * Reading happens in three stages, each catching a different class of mistakes:
 *
 * <ol>
 * <li>the file is parsed and validated against the bundled XML Schema, which
 * rejects files that
 * are not well-formed or do not have the expected structure;
 * <li>the textual values (sizes, slice, coordinate lists, ...) are parsed and
 * cross-checked;
 * <li>the rule is compiled, which proves that it is syntactically correct and
 * only refers to
 * neighborhoods that exist.
 * </ol>
 *
 * <p>
 * A file that passes all three is guaranteed to be instantiable by
 * {@link AutomatonFactory}.
 * Whatever goes wrong is reported as a {@link DefinitionException} whose
 * message starts with the
 * name of the file.
 *
 * <p>
 * The parser is configured defensively (no DTD, no external entity, no
 * XInclude), so opening an
 * untrusted file cannot make the application read other files or reach the
 * network.
 */
public final class XmlDefinitionReader {

    /** Name of the root element of a definition file. */
    public static final String ROOT_ELEMENT = "automaton";

    private static final String SCHEMA_RESOURCE = "automaton.xsd";

    private XmlDefinitionReader() {
    }

    /**
     * Reads a definition file.
     *
     * @param file path of the XML file
     * @return the validated definition
     * @throws DefinitionException if the file cannot be read or does not describe a
     *                             valid automaton
     */
    public static AutomatonDefinition read(Path file) throws DefinitionException {
        String source = file.getFileName().toString();
        try (InputStream input = Files.newInputStream(file)) {
            return read(input, source);
        } catch (IOException e) {
            throw new DefinitionException(source + ": cannot read the file (" + e.getMessage() + ")", e);
        }
    }

    /**
     * Reads a definition from a stream.
     *
     * @param input  the XML document
     * @param source name used to refer to the document in error messages
     * @return the validated definition
     * @throws DefinitionException if the document does not describe a valid
     *                             automaton
     */
    public static AutomatonDefinition read(InputStream input, String source)
            throws DefinitionException {
        Element root = parse(input, source);
        try {
            AutomatonDefinition definition = convert(root);
            AutomatonFactory.compileRule(definition);
            return definition;
        } catch (IllegalArgumentException e) {
            throw new DefinitionException(source + ": " + e.getMessage(), e);
        }
    }

    /**
     * Tells, cheaply, whether a file is meant to be an automaton definition, i.e.
     * whether it is an
     * XML document whose root element is {@code <automaton>}. Other XML files that
     * happen to sit
     * in the same directory (a Maven {@code pom.xml}, for instance) are thereby
     * ignored.
     *
     * @param file the file to probe
     * @return {@code true} if the file starts like a definition; {@code false}
     *         otherwise,
     *         including when it cannot be read
     */
    public static boolean looksLikeDefinition(Path file) {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try (InputStream input = Files.newInputStream(file)) {
            XMLStreamReader reader = factory.createXMLStreamReader(input);
            try {
                while (reader.hasNext()) {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                        return ROOT_ELEMENT.equals(reader.getLocalName());
                    }
                }
                return false;
            } finally {
                reader.close();
            }
        } catch (IOException | XMLStreamException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------------------------------
    // Stage 1: XML parsing and schema validation
    // ---------------------------------------------------------------------------------------

    private static Element parse(InputStream input, String source) throws DefinitionException {
        try {
            DocumentBuilder builder = SecureFactories.DOCUMENTS.newDocumentBuilder();
            builder.setErrorHandler(new Strict());
            Document document = builder.parse(input);
            return document.getDocumentElement();
        } catch (SAXParseException e) {
            throw new DefinitionException(
                    source + ": line " + e.getLineNumber() + ": " + plain(e.getMessage()), e);
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new DefinitionException(source + ": " + plain(e.getMessage()), e);
        }
    }

    /**
     * Strips the "cvc-xxx.n.n: " code that the validator puts in front of its
     * messages.
     */
    private static String plain(String message) {
        return message == null ? "invalid XML" : message.replaceFirst("^cvc-[\\w.\\-]+:\\s*", "");
    }

    /**
     * Makes every problem reported by the XML parser fatal, including schema
     * violations.
     */
    private static final class Strict implements ErrorHandler {
        @Override
        public void warning(SAXParseException exception) {
            // Warnings do not prevent the document from being understood.
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    }

    /** Lazily built, shared XML factories. Both are thread-safe once configured. */
    private static final class SecureFactories {

        static final DocumentBuilderFactory DOCUMENTS = documentBuilderFactory();

        private static DocumentBuilderFactory documentBuilderFactory() {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setXIncludeAware(false);
                factory.setExpandEntityReferences(false);
                factory.setIgnoringComments(true);
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setSchema(schema());
                return factory;
            } catch (ParserConfigurationException | SAXException e) {
                throw new IllegalStateException("cannot configure the XML parser", e);
            }
        }

        private static Schema schema() throws SAXException {
            URL resource = XmlDefinitionReader.class.getResource(SCHEMA_RESOURCE);
            if (resource == null) {
                throw new IllegalStateException("missing resource " + SCHEMA_RESOURCE);
            }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newSchema(resource);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Stage 2: from elements to a definition
    // ---------------------------------------------------------------------------------------

    private static AutomatonDefinition convert(Element root) {
        int dimension = parseDimension(text(root, "dimension").orElseThrow());
        Shape shape = parseShape(text(root, "size").orElseThrow(), dimension);

        AutomatonDefinition.Builder builder = AutomatonDefinition.builder()
                .name(root.getAttribute("name").strip())
                .description(text(root, "description").map(XmlDefinitionReader::flatten).orElse(""))
                .shape(shape)
                .boundary(text(root, "boundary").map(BoundaryCondition::parse).orElse(BoundaryCondition.FIXED))
                .slice(text(root, "slice").map(Slice::parse).orElse(Slice.centered(shape)))
                .rule(dedent(text(root, "rule").orElseThrow()));

        child(root, "neighborhoods").ifPresent(neighborhoods -> {
            for (Element neighborhood : children(neighborhoods, "neighborhood")) {
                String name = neighborhood.getAttribute("name").strip();
                List<Coordinates> offsets;
                try {
                    offsets = CoordinateLists.parse(neighborhood.getTextContent());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("neighborhood " + name + ": " + e.getMessage());
                }
                builder.neighborhood(name, offsets);
            }
        });

        Element initial = child(root, "initial").orElseThrow();
        OptionalLong seed = initial.hasAttribute("seed")
                ? OptionalLong.of(Long.parseLong(initial.getAttribute("seed").strip()))
                : OptionalLong.empty();
        builder.initialState(InitialState.parse(initial.getTextContent(), seed));

        child(root, "colors").ifPresent(colors -> {
            for (Element state : children(colors, "state")) {
                builder.color(
                        Integer.parseInt(state.getAttribute("value").strip()),
                        Integer.parseInt(state.getAttribute("color").strip().substring(1), 16));
            }
        });
        return builder.build();
    }

    private static int parseDimension(String text) {
        try {
            return Integer.parseInt(text.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("dimension " + text.strip() + " is too large");
        }
    }

    /**
     * Reads sizes written as {@code 96, 64}, {@code (96, 64)} or {@code 96 x 64}.
     */
    private static Shape parseShape(String text, int dimension) {
        String body = text.strip();
        if (body.startsWith("(") && body.endsWith(")")) {
            body = body.substring(1, body.length() - 1).strip();
        }
        String[] parts = body.split("\\s*[,x×]\\s*|\\s+");
        int[] sizes = new int[parts.length];
        for (int axis = 0; axis < parts.length; axis++) {
            try {
                sizes[axis] = Integer.parseInt(parts[axis]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "invalid size '" + text.strip() + "': '" + parts[axis] + "' is not an integer");
            }
        }
        if (sizes.length != dimension) {
            throw new IllegalArgumentException(
                    "the size lists " + sizes.length + " value(s) but the grid has " + dimension
                            + " dimension(s)");
        }
        return Shape.of(sizes);
    }

    /**
     * Removes the indentation a multi-line rule owes to its position in the XML
     * file, keeping the
     * indentation of its lines relative to each other.
     */
    private static String dedent(String text) {
        List<String> lines = text.strip().lines().toList();
        int margin = text.lines()
                .filter(line -> !line.isBlank())
                .mapToInt(line -> line.length() - line.stripLeading().length())
                .min()
                .orElse(0);
        StringBuilder dedented = new StringBuilder();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).stripTrailing();
            // The first line lost its indentation to strip() above.
            int indent = index == 0 ? 0 : Math.min(margin, line.length() - line.stripLeading().length());
            dedented.append(index == 0 ? "" : "\n").append(line.substring(indent));
        }
        return dedented.toString();
    }

    private static String flatten(String text) {
        return text.strip().replaceAll("\\s+", " ");
    }

    private static Optional<String> text(Element parent, String name) {
        return child(parent, name).map(Element::getTextContent);
    }

    private static Optional<Element> child(Element parent, String name) {
        return children(parent, name).stream().findFirst();
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> elements = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && name.equals(element.getLocalName())) {
                elements.add(element);
            }
        }
        return elements;
    }
}
