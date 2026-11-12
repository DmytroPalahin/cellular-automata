package fr.uspn.cellula.ui.swing;

import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.config.DefinitionLibrary;
import fr.uspn.cellula.config.DefinitionLibrary.Entry;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

/**
 * The start-up menu: lists the automaton definition files of a directory and launches the
 * simulation of the one the user picks.
 *
 * <p>Invalid files are listed too, with the reason why they were rejected, so that a typo in a
 * definition is discovered here rather than by the absence of the file.
 */
public final class LauncherFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final DefaultListModel<Entry> entries = new DefaultListModel<>();
    private final JList<Entry> list = new JList<>(entries);
    private final JTextArea details = new JTextArea();
    private final JLabel directoryLabel = new JLabel();
    private final JButton runButton = new JButton("Run simulation");
    private transient Path directory;

    /**
     * Creates the menu.
     *
     * @param directory the directory whose definition files are listed
     */
    public LauncherFrame(Path directory) {
        super("Cellula");
        this.directory = directory.toAbsolutePath().normalize();

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new EntryRenderer());
        list.addListSelectionListener(event -> showSelection());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    runSelection();
                }
            }
        });
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "run");
        list.getActionMap().put("run", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent event) {
                runSelection();
            }
        });

        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        details.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JScrollPane listPane = new JScrollPane(
                list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPane, new JScrollPane(details));
        split.setDividerLocation(300);
        split.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        setLayout(new BorderLayout());
        add(header(), BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);
        getRootPane().setDefaultButton(runButton);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(860, 540));
        pack();
        setLocationRelativeTo(null);
        refresh();
    }

    private JComponent header() {
        JLabel title = new JLabel("Choose an automaton");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize() + 5f));

        JButton choose = new JButton("Change folder…");
        choose.addActionListener(event -> chooseDirectory());
        JButton rescan = new JButton("Refresh");
        rescan.setToolTipText("Read the folder again");
        rescan.addActionListener(event -> refresh());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(choose);
        buttons.add(rescan);

        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.add(title, BorderLayout.NORTH);
        text.add(directoryLabel, BorderLayout.SOUTH);

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));
        header.add(text, BorderLayout.CENTER);
        header.add(buttons, BorderLayout.EAST);
        return header;
    }

    private JComponent footer() {
        JButton quit = new JButton("Quit");
        quit.addActionListener(event -> dispose());
        runButton.addActionListener(event -> runSelection());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        footer.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        footer.add(quit);
        footer.add(runButton);
        return footer;
    }

    private void refresh() {
        directoryLabel.setText(directory.toString());
        entries.clear();
        try {
            List<Entry> found = DefinitionLibrary.scan(directory);
            found.forEach(entries::addElement);
        } catch (IOException e) {
            details.setText("Cannot list " + directory + ":\n" + e.getMessage());
            runButton.setEnabled(false);
            return;
        }
        if (entries.isEmpty()) {
            details.setText("""
                    No automaton definition in this folder.

                    A definition is an XML file whose root element is <automaton>.
                    The 'examples' folder of the project contains several of them.""");
            runButton.setEnabled(false);
        } else {
            list.setSelectedIndex(0);
        }
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser(directory.toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Folder of automaton definitions");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            directory = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
            refresh();
        }
    }

    private void showSelection() {
        Entry entry = list.getSelectedValue();
        if (entry == null) {
            runButton.setEnabled(false);
            return;
        }
        switch (entry) {
            case Entry.Loaded loaded -> {
                details.setText(describe(loaded.definition()));
                runButton.setEnabled(true);
            }
            case Entry.Broken broken -> {
                details.setText("This file is not a valid definition.\n\n" + broken.problem());
                runButton.setEnabled(false);
            }
        }
        details.setCaretPosition(0);
    }

    private void runSelection() {
        if (list.getSelectedValue() instanceof Entry.Loaded loaded) {
            try {
                new SimulationFrame(loaded.definition()).setVisible(true);
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(
                        this, e.getMessage(), "Cannot start the simulation", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static String describe(AutomatonDefinition definition) {
        StringBuilder text = new StringBuilder();
        text.append(definition.name()).append('\n');
        text.append(definition.summary()).append('\n');
        if (!definition.description().isEmpty()) {
            text.append('\n').append(definition.description()).append('\n');
        }
        text.append("\nRule\n").append(definition.rule().strip().indent(2));
        if (!definition.neighborhoods().isEmpty()) {
            text.append("\nUser-defined neighborhoods\n");
            definition.neighborhoods().forEach((name, offsets) ->
                    text.append("  ").append(name).append(" = ").append(offsets).append('\n'));
        }
        String initial = definition.initialState().describe();
        text.append("\nInitial state\n  ")
                .append(initial.length() <= 300 ? initial : initial.substring(0, 300) + " …")
                .append('\n');
        if (definition.dimension() >= 3) {
            text.append("\nDisplayed slice\n  ").append(definition.slice()).append('\n');
        }
        return text.toString();
    }

    /** Shows an entry on two lines: its name, then where it comes from. */
    private static final class EntryRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean selected, boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected, focused);
            String html = switch ((Entry) value) {
                case Entry.Loaded loaded -> "<html><b>" + escape(loaded.definition().name())
                        + "</b><br><span style='color:gray'>" + escape(loaded.fileName()) + " · "
                        + escape(loaded.definition().dimension() + "D · " + loaded.definition().shape())
                        + "</span></html>";
                case Entry.Broken broken -> "<html><b>" + escape(broken.fileName())
                        + "</b><br><span style='color:#B91C1C'>invalid definition</span></html>";
            };
            setText(html);
            setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            return this;
        }

        private static String escape(String text) {
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
