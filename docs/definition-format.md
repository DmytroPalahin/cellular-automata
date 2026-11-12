# Definition format

An automaton is described by an XML file whose root element is `<automaton>`. The application
lists every such file of a directory in its menu; other XML files (a `pom.xml`, for instance) are
ignored.

- [A complete example](#a-complete-example)
- [Elements](#elements)
- [Notations](#notations)
- [Validation](#validation)

## A complete example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<automaton name="Everything at once">
  <description>A 3D automaton that uses every feature of the format.</description>

  <dimension>3</dimension>
  <size>40, 40, 40</size>
  <boundary>periodic</boundary>
  <slice>(20, :, :)</slice>

  <neighborhoods>
    <neighborhood name="G3">{(0, 0, 0), (1, 1, 0), (-1, 0, 0)}</neighborhood>
  </neighborhoods>

  <rule>
    SI(EQ(COMPTER(G0), 1),
       SUPEQ(COMPTER(G26*), 13),
       EQ(COMPTER(G3), 2))
  </rule>

  <initial seed="42">RANDOM(50)</initial>

  <colors>
    <state value="0" color="#0F172A"/>
    <state value="1" color="#38BDF8"/>
  </colors>
</automaton>
```

The children of `<automaton>` may come in any order, and each appears at most once.

## Elements

| Element | Required | Content |
| --- | :---: | --- |
| `automaton` `name="..."` | yes | Root element. The `name` attribute is the display name. |
| `description` | no | Free text shown in the menu. |
| `dimension` | yes | Number of dimensions of the grid: a positive integer. |
| `size` | yes | Extent of the grid along each axis: as many positive integers as dimensions. |
| `boundary` | no | `fixed` (default) or `periodic`. |
| `slice` | no | The plane to display, for grids of three dimensions or more. |
| `neighborhoods` | no | User-defined neighborhoods, one `neighborhood` element each. |
| `rule` | yes | The evolution rule, in the [rule language](rule-language.md). |
| `initial` `seed="..."` | yes | The cells that start in state 1, or `RANDOM(k)`. |
| `colors` | no | Display colors, one `state` element per state. |

### `dimension` and `size`

`size` lists one extent per axis, separated by commas, spaces or `x`:
`64, 96`, `(64, 96)` and `64 x 96` are equivalent. A grid may hold at most 64 million cells.

Coordinates are array indices starting at 0. In the window, the first displayed axis runs down
the screen (rows) and the second runs to the right (columns): for a 2D grid, `(row, column)`.

### `boundary`

What a rule sees beyond the edge of the grid.

| Value | Meaning |
| --- | --- |
| `fixed` | Every position outside the grid holds a cell that is permanently off. This is what the subject specifies, and the default. |
| `periodic` | Opposite edges are glued together: the grid is a torus. |

### `slice`

A grid of three dimensions or more cannot be shown whole: the window displays one plane of it.
The slice pins every axis but two to a coordinate and marks the two displayed axes with `:`,
as in Matlab. `(37, :, :)` is the plane where the first coordinate equals 37.

- Exactly two axes must be free. The first free axis gives the rows of the picture, the second
  its columns.
- Pinned coordinates must lie inside the grid.
- When the element is absent, the first two axes are displayed and the others are pinned at their
  middle. The plane can be moved from the window while the simulation runs.
- For grids of one or two dimensions the element is unnecessary.

### `neighborhoods`

Each `neighborhood` element declares a neighborhood by listing the coordinates of the neighbors of
a cell located at the origin, exactly as in the subject:

```xml
<neighborhood name="G4*">{(-1, 0), (1, 0), (0, -1), (0, 1)}</neighborhood>
```

- The name is a capital `G`, a number and an optional star: `G3`, `G12*`.
- Every offset has as many coordinates as the grid has dimensions.
- Including the origin `(0, ..., 0)` makes the cell part of its own neighborhood.
- A user-defined neighborhood takes precedence over a predefined one of the same name.

See [the predefined neighborhoods](rule-language.md#neighborhoods).

### `rule`

The expression giving the next state of a cell. It may span several lines; the indentation is
free. See the [rule language](rule-language.md).

### `initial`

Either form of the subject:

| Content | Meaning |
| --- | --- |
| `RANDOM(k)` | Each cell is on with probability *k*%, *k* between 0 and 100. |
| a list of coordinates | The listed cells are on, all the others off. An empty list gives an empty grid. |

```xml
<initial>RANDOM(15)</initial>
<initial>(0, 1), (1, 2), (2, 0), (2, 1), (2, 2)</initial>
<initial>(128)</initial>
```

The optional `seed` attribute (an integer) fixes the random generator, so that `RANDOM(k)` gives
the same grid at every run. Without it, each start and each press on **Randomize** draws a new
grid.

### `colors`

Each `state` element gives the color of one state as `#RRGGBB`. States without an entry keep
their default color: white for 0, blue for 1, then a series of distinct colors.

## Notations

**Coordinates** are written as tuples: `(3, 4)`. On a one-dimensional grid the parentheses are
optional: `128` and `(128)` are the same position.

**Lists of coordinates** separate tuples with commas. Surrounding braces are optional:
`{(0, 0), (1, 1)}` and `(0, 0), (1, 1)` are equivalent.

**White space** and line breaks are insignificant everywhere.

## Validation

A file goes through three checks, and the first problem found is reported with the name of the
file:

1. **Structure**, against the XML Schema bundled with the application
   ([`automaton.xsd`](../src/main/resources/fr/uspn/cellula/config/automaton.xsd)): unknown or
   duplicated elements, missing mandatory ones, malformed colors or neighborhood names. These
   messages carry a line number.
2. **Consistency**: the number of sizes against the dimension, the slice against the grid, the
   offsets of the neighborhoods against the dimension, the initial cells against the grid.
3. **The rule**, by compiling it: syntax, number of arguments, existence of the neighborhoods for
   the dimension of the grid. These messages quote the faulty line with a caret.

Check files without opening them:

```bash
java -jar target/cellula.jar validate examples/*.xml
```

For safety, document type declarations (`<!DOCTYPE ...>`) and external entities are refused: a
definition file cannot make the application read other files or reach the network.
