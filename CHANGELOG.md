# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-11-12

First release. It covers every requirement of the subject, the bonus question, and a few
extensions.

### Added

- **Grid** of any dimension chosen at run time, stored in a recursively defined array
  (`DynamicArrayND`); cells outside the grid count as permanently off.
- **Neighborhoods** `G0`, `G2`, `G4`, `G8`, `G6`, `G26` and their starred variants, exposed as
  `Iterable` walks, and user-defined neighborhoods given as lists of offsets.
- **Rule language** with the operators `ET`, `OU`, `NON`, `SUP`, `SUPEQ`, `EQ`, `COMPTER`, `ADD`,
  `SUB`, `MUL` and `SI`, parsed into a syntax tree; syntax errors are reported with line, column
  and a caret under the faulty token.
- **Definition files** in XML: dimension, size, displayed slice, neighborhoods, rule, initial
  cells or `RANDOM(k)`. Files are validated against an XML Schema, checked for consistency, and
  their rule is compiled before a file is accepted.
- **Simulation window** (Swing): play, pause, step, reset, speed control, PNG export. Automata of
  one dimension are shown as space-time diagrams, automata of three dimensions or more through a
  plane written like `(37, :, :)`.
- **Menu** listing the definition files of a directory, as a window or in the terminal.
- **Example definitions**, including the Game of Life from `RANDOM(15)` and the Sierpiński
  triangle required by the subject.
- **Report** in LaTeX with class diagrams, and the subject transcribed in French.

#### Beyond the subject

- Predefined neighborhoods are generated for every dimension, so grids of four dimensions and
  more work out of the box (`G8` and `G80` in 4D).
- `periodic` boundary condition, turning the grid into a torus.
- `ETAT()` operator and per-state colors, for automata with more than two states.
- Detection of convergence and of cycles up to 1024 generations long.
- Drawing cells with the mouse; moving the displayed slice while the simulation runs.
- Command-line interface with batch commands `list`, `validate`, `print` and `render`, usable
  without a display.
- English aliases for the operators (`IF`, `AND`, `OR`, `NOT`, `GT`, `GE`, `COUNT`, `STATE`).
- Reproducible random initial states with the `seed` attribute.

#### Bonus question

- `explore` command: searches the one-dimensional rules of radius 2 for fractal and chaotic
  ones, using the box-counting dimension and the compression ratio of their space-time picture,
  and discards those a rule of radius 1 could reproduce.
- Two rules found this way ship as `examples/radius2-fractal.xml` and
  `examples/radius2-chaotic.xml`.

[Unreleased]: ../../compare/v1.0.0...HEAD
[1.0.0]: ../../releases/tag/v1.0.0
