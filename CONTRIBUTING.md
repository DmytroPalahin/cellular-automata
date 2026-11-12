# Contributing to Cellula

Thank you for taking the time to contribute. This document explains how to build the project,
what is expected of a change, and how to do a few common things.

- [Getting started](#getting-started)
- [Making a change](#making-a-change)
- [Code conventions](#code-conventions)
- [Tests](#tests)
- [Commit messages](#commit-messages)
- [Recipes](#recipes)
- [Releasing](#releasing)

## Getting started

You need a JDK, version 21 or newer. Maven comes with the wrapper.

```bash
./mvnw verify
```

This single command is the definition of "it works": it compiles the code with every lint enabled
and treated as an error, runs the whole test suite, builds `target/cellula.jar` and writes the
coverage report to `target/site/jacoco/index.html`. Continuous integration runs exactly the same
command on Linux, macOS and Windows.

To try the application:

```bash
java -jar target/cellula.jar --dir examples
```

The project is a standard Maven project and opens as such in IntelliJ IDEA, Eclipse and VS Code.

## Making a change

1. Open an issue first for anything larger than a fix, so the approach can be discussed before
   the work is done.
2. Create a branch from `main`: `feat/hexagonal-display`, `fix/slice-off-by-one`, ...
3. Make the change, with tests.
4. Update the documentation that the change makes inaccurate: `README.md`, the guides in `docs/`,
   the Javadoc.
5. Add a line to the "Unreleased" section of [CHANGELOG.md](CHANGELOG.md).
6. Check that `./mvnw verify` passes, then open a pull request that explains the *why*.

Keep pull requests focused: one concern per pull request is easier to review and to revert.

## Code conventions

The code base is small enough to be read: when in doubt, do as the neighboring code does.

**Architecture**

- Dependencies between packages point one way, as described in
  [docs/architecture.md](docs/architecture.md). The model (`grid`, `neighborhood`, `rule`,
  `automaton`) must not import `config`, `simulation`, `ui` or `cli`, and nothing outside
  `ui.swing` may import Swing.
- No runtime dependency other than the JDK. A new library needs a very good reason.
- Prefer immutable types (records, `final` fields, unmodifiable collections). Mutable state lives
  in few, well-identified places: `GridCell`, `CellularAutomaton`, `Simulation`.
- Validate at the boundaries. Constructors and factories reject invalid arguments with a message
  that tells the user what to fix; inner code can then trust its inputs.

**Style**

- Java 21. Use records, sealed interfaces, pattern matching and text blocks where they make the
  code clearer.
- Four spaces, no tabs, lines up to 110 columns, one top-level type per file
  (see `.editorconfig`).
- No wildcard imports, no fully qualified names in code.
- Names are spelled out: `neighborhood`, not `nbh`.
- Every public type and method has a Javadoc comment. It says what the thing is for and what its
  contract is, not how it is implemented.
- Comments inside methods explain *why*, when the code cannot.
- Error messages are written for the person who made the mistake: lower case, no trailing period,
  with the offending value and, when possible, the valid alternatives.

**Exceptions**

- `IllegalArgumentException` (and its subclasses `RuleSyntaxException`,
  `UnknownNeighborhoodException`) for invalid values.
- `DefinitionException`, a checked exception, for anything wrong with an input file: every caller
  has to handle it, usually by showing the message.
- Never swallow an exception silently.

## Tests

Tests are written with JUnit and live next to the code they cover, in the same package under
`src/test/java`.

- A change in behavior comes with a test that fails without it.
- Test names are sentences that state the behavior: `aBlinkerOscillatesWithPeriodTwo`.
- Prefer checking behavior through the public API over checking implementation details.
- Tests must be deterministic. Randomness is seeded, and tests of `Simulation` synchronize on
  `simulation.state()` instead of sleeping.
- Tests never open a window, so the suite runs on a machine without a display.

The Swing windows have their own tests (`SwingSmokeTest`), which build the real windows without
showing them. They are skipped in a headless JVM. Run them on a workstation with:

```bash
./mvnw verify -Dtest.headless=false
```

Line coverage is above 95% outside `ui.swing`; please keep it there.

## Commit messages

Commits follow [Conventional Commits](https://www.conventionalcommits.org/):

```text
<type>(<scope>): <summary in the imperative, lower case, no period>

<body: what and why, wrapped at 72 columns>
```

| Type | For |
| --- | --- |
| `feat` | a new feature |
| `fix` | a bug fix |
| `docs` | documentation only |
| `test` | tests only |
| `refactor` | a change that neither fixes a bug nor adds a feature |
| `perf` | a performance improvement |
| `build`, `ci`, `chore` | build system, continuous integration, housekeeping |

The scope is usually a package: `grid`, `rule`, `config`, `ui`, `cli`, `explore`.

```text
feat(rule): add the MOD operator

Rules for automata with more than two states often need to cycle
through the states; MOD(a, b) makes that expressible without a chain
of SI.
```

## Recipes

### Adding an operator to the rule language

Say we want `MOD(a, b)`, the remainder of `a` divided by `b`.

1. Create `src/main/java/fr/uspn/cellula/rule/expression/Modulo.java`:

   ```java
   /** {@code MOD(a, b)}: yields the remainder of {@code a} divided by {@code b}, or 0 if b is 0. */
   public final class Modulo extends BinaryOperator {

       public static final String SYMBOL = "MOD";

       public Modulo(Expression left, Expression right) {
           super(SYMBOL, left, right);
       }

       @Override
       protected int apply(int left, int right) {
           return right == 0 ? 0 : Math.floorMod(left, right);
       }
   }
   ```

2. Register it in `OperatorCatalog.standard()`:

   ```java
   catalog.register(binary(Modulo.SYMBOL, Modulo::new));
   ```

3. Add its truth table to `OperatorsTest`, and a line to the table of
   [docs/rule-language.md](docs/rule-language.md).

The parser needs no change: arity checking, error messages and canonical printing come from the
catalog and from `Operator`.

### Adding an example

1. Write the definition in `examples/`, with an XML comment explaining what it demonstrates and a
   `<description>`.
2. Check it: `java -jar target/cellula.jar validate examples/my-example.xml`.
3. Add it to the table in `README.md`.

`ExamplesTest` picks up every file of `examples/` automatically and fails if one is invalid.

### Regenerating the pictures

```bash
./scripts/figures.sh
```

```bash
./scripts/screenshots.sh
```

The first renders the automata of `docs/images` with the application in batch mode; the second
captures the windows and needs a display.

### Building the report

```bash
make -C report
```

needs a LaTeX distribution with `latexmk`. The PDF is committed, so this is only needed after
editing `report/`.

## Releasing

1. Move the "Unreleased" entries of `CHANGELOG.md` under a new version heading with today's date.
2. Set the version in `pom.xml`.
3. Commit as `chore(release): x.y.z` and tag the commit `vx.y.z`.
4. `./scripts/package-submission.sh` builds `dist/cellula-x.y.z.zip`.

Versions follow [Semantic Versioning](https://semver.org/). The format of definition files and
the command line are the public interface: a change that breaks either is a major version.
