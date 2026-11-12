package fr.uspn.cellula.ui.swing;

import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.config.InitialState;
import fr.uspn.cellula.grid.Cell;
import fr.uspn.cellula.grid.Frame;
import fr.uspn.cellula.grid.Shape;
import fr.uspn.cellula.grid.Slice;
import fr.uspn.cellula.render.FrameImages;
import fr.uspn.cellula.render.Palette;
import fr.uspn.cellula.simulation.Simulation;
import fr.uspn.cellula.simulation.Snapshot;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * The window that shows one running automaton.
 *
 * <p>The window is a thin view over a {@link Simulation}. User actions are forwarded to the
 * simulation as commands; the simulation answers by publishing {@link Snapshot}s, which this class
 * moves onto the event dispatch thread before touching any component. When snapshots arrive
 * faster than Swing can paint, intermediate ones are dropped and only the latest is shown.
 */
public final class SimulationFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    /** Slider position that means "no pause between generations". */
    private static final int UNLIMITED_SPEED = 61;

    private final transient AutomatonDefinition definition;
    private final transient Simulation simulation;
    private final transient AtomicReference<Snapshot> pending = new AtomicReference<>();
    private final transient Map<Integer, JSpinner> sliceSpinners = new LinkedHashMap<>();
    private final FrameCanvas canvas;
    private final JButton playButton = new JButton("Play");
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel sliceLabel = new JLabel();
    private final JLabel speedLabel = new JLabel();

    private transient Snapshot latest;
    private boolean syncingControls;
    private int paintState = Cell.ON;

    /**
     * Creates the window and its simulation, paused at generation 0.
     *
     * @param definition the automaton to show
     * @throws IllegalArgumentException if the definition cannot be instantiated
     */
    public SimulationFrame(AutomatonDefinition definition) {
        super(definition.name() + " — Cellula");
        this.definition = definition;
        this.simulation = new Simulation(definition);
        boolean spaceTime = definition.dimension() == 1;
        this.canvas = new FrameCanvas(Palette.of(definition.colors()), spaceTime);

        Shape shape = definition.shape();
        Slice slice = definition.slice();
        int columns = slice.columns(shape);
        int rows = spaceTime ? Math.max(120, columns * 2 / 3) : slice.rows(shape);
        canvas.setPreferredSize(FrameCanvas.comfortableSize(rows, columns));
        canvas.setCellListener(this::paintCell);

        JPanel north = new JPanel(new BorderLayout());
        north.add(toolBar(), BorderLayout.NORTH);
        if (!slice.pinnedAxes().isEmpty()) {
            north.add(sliceBar(shape, slice), BorderLayout.SOUTH);
        }

        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(statusBar(), BorderLayout.SOUTH);
        bindKeys();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                simulation.close();
            }
        });
        pack();
        setMinimumSize(new Dimension(560, 420));
        setLocationByPlatform(true);

        simulation.setInterval(intervalFor(10));
        simulation.addListener(this::enqueue);
    }

    // ---------------------------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------------------------

    private JToolBar toolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        playButton.setToolTipText("Start or pause the automatic advance (Space)");
        playButton.setFocusable(false);
        playButton.addActionListener(event -> togglePlay());
        bar.add(playButton);
        bar.add(button("Step", "Advance by one generation (Right arrow)", simulation::step));
        bar.add(button("Reset", "Go back to generation 0 (R)", simulation::reset));
        if (definition.initialState() instanceof InitialState.RandomFill) {
            bar.add(button("Randomize", "Draw a new random initial state", simulation::reseed));
        }
        bar.add(button("Clear", "Switch every cell off, to draw a pattern by hand", simulation::clear));
        bar.addSeparator();

        JSlider speed = new JSlider(1, UNLIMITED_SPEED, 10);
        speed.setFocusable(false);
        speed.setMaximumSize(new Dimension(160, 28));
        speed.setToolTipText("Generations per second");
        speed.addChangeListener(event -> {
            simulation.setInterval(intervalFor(speed.getValue()));
            speedLabel.setText(describeSpeed(speed.getValue()));
        });
        speedLabel.setText(describeSpeed(speed.getValue()));
        speedLabel.setPreferredSize(new Dimension(70, 20));
        bar.add(new JLabel("Speed "));
        bar.add(speed);
        bar.add(speedLabel);
        bar.addSeparator();

        JCheckBox grid = new JCheckBox("Grid", canvas.gridLines());
        grid.setFocusable(false);
        grid.setToolTipText("Separate cells with thin lines when they are large enough");
        grid.addActionListener(event -> canvas.setGridLines(grid.isSelected()));
        bar.add(grid);

        bar.add(Box.createHorizontalGlue());
        bar.add(button("Rule…", "Show the definition of this automaton", this::showDefinition));
        bar.add(button("Export PNG…", "Save the current picture as an image", this::exportImage));
        return bar;
    }

    private JComponent sliceBar(Shape shape, Slice slice) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 6, 4, 6));
        bar.add(new JLabel("Slice"));
        sliceLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, sliceLabel.getFont().getSize()));
        sliceLabel.setText(slice.toString());
        bar.add(sliceLabel);
        for (int axis : slice.pinnedAxes()) {
            JSpinner spinner = new JSpinner(
                    new SpinnerNumberModel(slice.pinnedAt(axis), 0, shape.size(axis) - 1, 1));
            spinner.setToolTipText("Coordinate of the displayed plane along axis " + axis);
            spinner.addChangeListener(event -> {
                if (!syncingControls && latest != null && latest.slice().isPresent()) {
                    simulation.setSlice(latest.slice().get().moved(axis, (Integer) spinner.getValue()));
                }
            });
            sliceSpinners.put(axis, spinner);
            bar.add(new JLabel("axis " + axis + " ="));
            bar.add(spinner);
        }
        return bar;
    }

    private JComponent statusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        // The status takes all the room the summary leaves, so it never needs a new layout pass
        // when its text grows.
        bar.add(statusLabel, BorderLayout.CENTER);
        bar.add(new JLabel(definition.summary()), BorderLayout.EAST);
        return bar;
    }

    private JButton button(String text, String toolTip, Runnable action) {
        JButton button = new JButton(text);
        button.setToolTipText(toolTip);
        button.setFocusable(false);
        button.addActionListener(event -> action.run());
        return button;
    }

    private void bindKeys() {
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggle-play", this::togglePlay);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "step", simulation::step);
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "reset", simulation::reset);
    }

    private void bindKey(KeyStroke stroke, String name, Runnable action) {
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, name);
        root.getActionMap().put(name, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    // ---------------------------------------------------------------------------------------
    // Simulation -> view
    // ---------------------------------------------------------------------------------------

    /** Called on the simulation thread: hands the snapshot over to the event dispatch thread. */
    private void enqueue(Snapshot snapshot) {
        if (pending.getAndSet(snapshot) == null) {
            SwingUtilities.invokeLater(this::showPending);
        }
    }

    private void showPending() {
        Snapshot snapshot = pending.getAndSet(null);
        if (snapshot == null) {
            return;
        }
        latest = snapshot;
        canvas.showFrame(snapshot.frame());
        playButton.setText(snapshot.running() ? "Pause" : "Play");
        statusLabel.setText(String.format(
                Locale.ROOT,
                "Generation %,d  ·  Population %,d (%.1f %%)  ·  Changed %,d  ·  %s, %s",
                snapshot.generation(),
                snapshot.population(),
                100 * snapshot.density(),
                snapshot.changedCells(),
                snapshot.regime().describe(),
                snapshot.running() ? "running" : "paused"));
        snapshot.slice().ifPresent(this::showSlice);
    }

    private void showSlice(Slice slice) {
        syncingControls = true;
        try {
            sliceLabel.setText(slice.toString());
            sliceSpinners.forEach((axis, spinner) -> spinner.setValue(slice.pinnedAt(axis)));
        } finally {
            syncingControls = false;
        }
    }

    // ---------------------------------------------------------------------------------------
    // View -> simulation
    // ---------------------------------------------------------------------------------------

    private void togglePlay() {
        if (latest != null && latest.running()) {
            simulation.pause();
        } else {
            simulation.play();
        }
    }

    /**
     * Draws on the grid: a press toggles the pointed cell, and dragging extends the new state to
     * the cells swept over. On a space-time diagram every row edits the current generation.
     */
    private void paintCell(int row, int column, boolean gestureStart) {
        Frame frame = canvas.frame();
        if (frame == null) {
            return;
        }
        if (gestureStart) {
            int shownRow = definition.dimension() == 1 ? frame.rows() - 1 : row;
            paintState = frame.stateAt(shownRow, column) == Cell.OFF ? Cell.ON : Cell.OFF;
        }
        simulation.setCell(row, column, paintState);
    }

    private static long intervalFor(int sliderValue) {
        return sliderValue >= UNLIMITED_SPEED ? 0 : 1000L / sliderValue;
    }

    private static String describeSpeed(int sliderValue) {
        return sliderValue >= UNLIMITED_SPEED ? " max" : " " + sliderValue + " gen/s";
    }

    private void showDefinition() {
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
        text.append("\nInitial state\n  ").append(abbreviate(definition.initialState().describe()));

        JTextArea area = new JTextArea(text.toString(), 18, 72);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(
                this, new JScrollPane(area), "Definition", JOptionPane.PLAIN_MESSAGE);
    }

    private static String abbreviate(String text) {
        return text.length() <= 400 ? text : text.substring(0, 400) + " …";
    }

    private void exportImage() {
        Snapshot snapshot = latest;
        if (snapshot == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PNG image", "png"));
        chooser.setSelectedFile(new File(String.format(
                Locale.ROOT, "%s-gen%d.png", slug(definition.name()), snapshot.generation())));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }
        Frame frame = snapshot.frame();
        int cellSize = Math.max(1, Math.min(16, 1200 / Math.max(frame.columns(), frame.rows())));
        try {
            FrameImages.writePng(
                    FrameImages.render(frame, Palette.of(definition.colors()), cellSize, canvas.gridLines()),
                    file.toPath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this, "Cannot write " + file + ":\n" + e.getMessage(), "Export failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String slug(String name) {
        String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return slug.isEmpty() ? "automaton" : slug;
    }
}
