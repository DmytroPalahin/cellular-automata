# Architecture

This document explains how Cellula is organized and why. The [report](../report/report.pdf)
covers the same ground with class diagrams.

- [Layers](#layers)
- [The model](#the-model)
- [From a file to an automaton](#from-a-file-to-an-automaton)
- [Running and displaying](#running-and-displaying)
- [Design patterns](#design-patterns)
- [Extending the application](#extending-the-application)

## Layers

```text
┌──────────────────────────────────────────────────────────────┐
│  cli        ui.swing        ui.text                          │  entry points
├──────────────────────────────────────────────────────────────┤
│  simulation        render        explore                     │  application services
├──────────────────────────────────────────────────────────────┤
│  config                                                      │  input files
├──────────────────────────────────────────────────────────────┤
│  automaton  →  rule  →  neighborhood  →  grid                │  model
└──────────────────────────────────────────────────────────────┘
```

A package only depends on packages below it (and, in the model row, to its right). Three
consequences are worth spelling out:

- **The model is pure.** `grid`, `neighborhood`, `rule` and `automaton` know nothing about XML,
  threads, Swing or the command line. They can be used as a library and are tested in isolation.
- **Swing is confined** to `ui.swing`. Removing that package leaves a working batch application.
- **The model has no dependency** other than the JDK, and neither has anything else.

| Package | Responsibility |
| --- | --- |
| `grid` | Storage of cells in any dimension; what lies beyond the edges; simultaneous update |
| `neighborhood` | Which cells a rule may look at around a cell |
| `rule` | The transition function; its syntax tree (`expression`) and its parser (`parser`) |
| `automaton` | Grid + rule + clock; convergence and cycle detection |
| `config` | The description of an automaton as data; XML reading, writing and validation |
| `simulation` | Running an automaton on its own thread; projecting a grid onto a picture |
| `render` | Pictures as images and as text |
| `ui.swing`, `ui.text` | The menu and the simulation, in a window or in a terminal |
| `explore` | The rule explorer of the bonus question |
| `cli` | The command-line interface |

## The model

### A grid of any dimension

Java fixes the dimension of an array in its type. `DynamicArrayND<T>` lifts the restriction with
the recursive definition the subject asks for: an array of dimension *n* and size
*k₁ × ... × kₙ* holds *k₁* arrays of dimension *n − 1*. The two cases of the definition are two
private subclasses, a `Branch` holding sub-arrays and a `Leaf` holding elements, behind one
abstract type. `get(int... indices)` descends one level per index.

`Grid` adds meaning to the storage:

- **Edges.** `cellAt(coordinates)` accepts any coordinates. Inside the grid it returns the stored
  `GridCell`; outside, it asks its `BoundaryCondition`. `FIXED` answers with `VoidCell.INSTANCE`,
  a cell that is off forever; `PERIODIC` wraps around. Rules never test bounds.
- **Simultaneity.** A cellular automaton updates all its cells at once. `evolve(transition)` does
  it in two phases: every cell computes its next state from the untouched current generation and
  stores it in a private buffer, then all cells switch. The first phase only reads shared state,
  so it runs in parallel on large grids.

### A neighborhood is a way of walking

The subject insists that a neighborhood is "nothing but a rule for traversing the cells around a
cell". That is the interface:

```java
public interface Neighborhood {
    Iterable<Cell> around(Grid grid, GridCell center);
    // name(), dimension(), size()
}
```

`OffsetNeighborhood`, the only implementation needed, walks a fixed list of offsets. An operator
consumes it with a `for` loop and never sees a coordinate.

The six predefined neighborhoods are not six classes. `Neighborhoods` generates the two families
they belong to, for any dimension, and `NeighborhoodCatalog` resolves a name such as `G26*`
against the dimension of the grid.

### A rule is a tree

A rule in the language of the subject is parsed into a tree of `Expression` nodes:

```text
Expression                      int evaluate(EvaluationContext)
├── Constant                    42
├── Count                       COMPTER(G8*)
├── CurrentState                ETAT()
└── Operator                    name + operands
    ├── UnaryOperator           NON
    ├── BinaryOperator          ET OU SUP SUPEQ EQ ADD SUB MUL
    └── Conditional             SI
```

Each node evaluates itself for a given cell. `UnaryOperator` and `BinaryOperator` evaluate their
operands and leave a one-line `apply` to their subclasses; `Count` is the only node that reads the
grid. Trees are immutable and have no side effects, which is what allows one tree to be evaluated
for many cells at the same time.

The engine itself only knows the `Rule` interface, `int nextState(Grid, GridCell)`.
`ExpressionRule` adapts a tree to it; a lambda does the job in tests.

### The automaton

`CellularAutomaton` is the grid, the rule and a generation counter. `step()` asks the grid to
evolve with the rule, then classifies what happened into a `Regime`: still evolving, converged
(the step changed nothing) or cycling (the grid is back in a known configuration; a
`CycleDetector` remembers a 64-bit fingerprint of each recent generation).

## From a file to an automaton

```text
file.xml ──XmlDefinitionReader──▶ AutomatonDefinition ──AutomatonFactory──▶ CellularAutomaton
```

`AutomatonDefinition` is an immutable record of plain data: sizes, the rule *as text*, offsets,
the initial state. Keeping this intermediate form separate from the live objects is what lets the
same definition be listed in a menu, instantiated several times, built in a test, or written back
to a file (`XmlDefinitionWriter`).

Reading is defensive and happens in three stages: XML Schema validation, consistency checks,
compilation of the rule. A file that passes is guaranteed to be instantiable. Parsing is hardened
against XML external entity attacks.

The rule parser is a classic pair: a `Lexer` producing tokens with their position, and a
recursive-descent `RuleParser`. The parser has no knowledge of any particular operator; it asks an
`OperatorCatalog` how many arguments a keyword takes and how to build its node.

## Running and displaying

### Threading

Computing a generation of a large grid takes time; a user interface must not freeze meanwhile.
`Simulation` therefore owns a single background thread and confines the automaton to it:

- every public method (`play`, `step`, `setCell`, ...) only posts a command to that thread;
- commands run one after the other, so no lock is needed;
- observers receive immutable `Snapshot`s, never the grid.

The Swing window forwards user actions as commands and repaints from snapshots, which it moves to
the event dispatch thread. If snapshots arrive faster than Swing can paint, it drops the
intermediate ones.

### Projections

A grid is turned into a flat `Frame` by a `Projection`:

- `PlaneProjection` shows the current generation through a `Slice` such as `(37, :, :)`;
- `SpaceTimeProjection` stacks the successive generations of a one-dimensional grid.

The window, the PNG export and the text output all draw frames, so they share one rendering path.

## Design patterns

| Pattern | Where | Why |
| --- | --- | --- |
| Composite | `DynamicArrayND` (`Branch`/`Leaf`); `Expression` | Recursive structures treated uniformly |
| Interpreter | `Expression.evaluate` | The syntax tree of a rule evaluates itself |
| Template method | `UnaryOperator`, `BinaryOperator` | Operand evaluation written once; subclasses supply `apply` |
| Iterator | `Neighborhood.around` | A neighborhood *is* a traversal |
| Strategy | `BoundaryCondition`, `Projection`, `Rule`, `InitialState` | Interchangeable behaviors behind one interface |
| Null object | `VoidCell` | No bounds checks in rules |
| Singleton | `VoidCell` (enum) | One shared instance |
| Factory | `AutomatonFactory`, `Neighborhoods`, `OperatorCatalog` | Construction separated from use |
| Builder | `AutomatonDefinition.Builder` | Readable construction of a nine-field record |
| Observer | `SimulationListener` | The view follows the simulation without coupling |
| Command | `cli.Command`; commands posted to `Simulation` | Uniform dispatch; serialized execution |

## Extending the application

**A new operator.** Write a class in `rule.expression` (usually three lines of `apply` in a
subclass of `BinaryOperator`) and register it in `OperatorCatalog.standard()`. The parser, the
error messages and the documentation of the arity come for free. See
[CONTRIBUTING.md](../CONTRIBUTING.md#adding-an-operator-to-the-rule-language).

**A new kind of neighborhood.** Implement `Neighborhood`. Anything that can enumerate cells
qualifies, for instance "all cells within distance *r*".

**A new boundary condition.** Add a constant to `BoundaryCondition` and say which cell stands
outside the grid (a mirror boundary would reflect the coordinates).

**A new input format.** Produce an `AutomatonDefinition` from it, with the builder. Nothing else
changes.

**A new display.** Implement `Projection` to produce frames, or consume `Snapshot`s from a
`SimulationListener`.
