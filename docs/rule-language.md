# Rule language

The rule of an automaton is an expression. It is evaluated for every cell at every time step, and
its value is the next state of the cell.

- [Grammar](#grammar)
- [Operators](#operators)
- [Neighborhoods](#neighborhoods)
- [Worked examples](#worked-examples)
- [Error messages](#error-messages)

## Grammar

Expressions use prefix notation: an operator name followed by its arguments in parentheses.

```text
rule         := expression
expression   := NUMBER | NAME '(' arguments ')'
arguments    := neighborhood                        -- for COMPTER
              | [ expression { ',' expression } ]   -- for every other operator
neighborhood := NAME                                -- G0, G8*, ...

NUMBER       := '-'? DIGIT+
NAME         := LETTER ( LETTER | DIGIT | '_' )* '*'?
```

- Every value is a 32-bit integer. Used as a truth value, 0 is false and anything else is true.
- Operator names are case-insensitive: `SI`, `Si` and `si` are the same operator.
- White space and line breaks are free, so long rules can be laid out over several lines.

## Operators

These are the operators of the subject. Arguments are expressions, so operators nest freely.

| Operator | Value | English alias |
| --- | --- | --- |
| `ET(a, b)` | 1 if `a` and `b` are both different from 0, else 0 | `AND` |
| `OU(a, b)` | 0 if `a` and `b` are both equal to 0, else 1 | `OR` |
| `NON(a)` | 1 if `a` equals 0, else 0 | `NOT` |
| `SUP(a, b)` | 1 if `a` is strictly greater than `b`, else 0 | `GT` |
| `SUPEQ(a, b)` | 1 if `a` is greater than or equal to `b`, else 0 | `GE` |
| `EQ(a, b)` | 1 if `a` equals `b`, else 0 | |
| `COMPTER(G)` | the number of cells in state 1 in the neighborhood `G` of the cell | `COUNT` |
| `ADD(a, b)` | `a + b` | |
| `SUB(a, b)` | `a - b` | |
| `MUL(a, b)` | `a * b` | |
| `SI(c, a, b)` | `a` if `c` is different from 0, else `b` | `IF` |

One operator is an extension to the subject:

| Operator | Value | English alias |
| --- | --- | --- |
| `ETAT()` | the state of the cell itself | `STATE` |

With two states `ETAT()` is the same as `COMPTER(G0)`. It becomes necessary when a rule produces
other states: `COMPTER` only counts cells in state 1, so it cannot tell a cell in state 2 from a
cell in state 0. See `examples/brians-brain.xml`.

Notes:

- `SI` only evaluates the branch it selects. Since expressions have no side effects this cannot be
  observed, but it saves the neighborhood walks of the other branch.
- Arithmetic wraps around on overflow, like Java `int` arithmetic.

## Neighborhoods

A neighborhood is the set of cells a rule may look at around the cell under examination. Its name
is a capital `G`, a number and an optional star.

### Predefined

| Name | Dimension | Cells |
| --- | :---: | --- |
| `G0` | any | the cell alone |
| `G2` | 1 | the cell, its left and its right neighbor |
| `G4` | 2 | the cell and its 4 neighbors above, below, left and right |
| `G8` | 2 | `G4` plus the 4 diagonal neighbors |
| `G6` | 3 | the cell and the 6 neighbors sharing a face with it |
| `G26` | 3 | the cell and the 26 neighbors touching it by a face, an edge or a corner |

Each of `G2`, `G4`, `G8`, `G6` and `G26` has a starred variant (`G8*`, ...) with the same cells
minus the cell itself.

A predefined neighborhood only exists in its dimension: using `G8` in a rule for a 3D grid is an
error, reported with the list of the names that do exist.

The six neighborhoods of the subject are instances of two families that the application
generates for every dimension *d*:

| Family | Number of neighbors | 1D | 2D | 3D | 4D |
| --- | :---: | :---: | :---: | :---: | :---: |
| faces only (von Neumann) | 2*d* | `G2` | `G4` | `G6` | `G8` |
| faces, edges and corners (Moore) | 3<sup>*d*</sup> − 1 | `G2` | `G8` | `G26` | `G80` |

So grids of four dimensions and more get their neighborhoods for free. Note that the same name
may mean different things in different dimensions: `G8` is the full neighborhood of a 2D cell but
the face neighborhood of a 4D cell.

### User-defined

A definition file can declare its own neighborhoods by listing offsets relative to the cell (see
[the definition format](definition-format.md#neighborhoods)):

```xml
<neighborhood name="G11">{(-1)}</neighborhood>
```

A neighborhood with a single cell turns `COMPTER` into "the state of that cell" (as 0 or 1),
which is how rules that treat left and right differently are written.

### Beyond the edge

On a `fixed` grid (the default) a position outside the grid counts as a cell that is off. On a
`periodic` grid it is the cell reached by wrapping around.

## Worked examples

### The example of the subject

> If a cell is alive and has 4 live neighbors or more in its 8-neighborhood, it dies, otherwise it
> stays alive. If a cell is dead and has exactly two live neighbors in its 8-neighborhood, it
> becomes alive, otherwise it stays dead.

```text
SI(EQ(COMPTER(G0), 1),
   SI(SUPEQ(COMPTER(G8), 4), 0, 1),
   SI(EQ(COMPTER(G8), 2), 1, 0))
```

`EQ(COMPTER(G0), 1)` tests whether the cell is alive; the two inner `SI` give the fate of a live
and of a dead cell. Since comparisons already yield 0 or 1, `SI(c, 1, 0)` can be shortened to `c`
and `SI(c, 0, 1)` to `NON(c)`.

### Game of Life

```text
SI(EQ(COMPTER(G0), 1),
   OU(EQ(COMPTER(G8*), 2), EQ(COMPTER(G8*), 3)),
   EQ(COMPTER(G8*), 3))
```

### Sierpiński triangle (rule 18)

The subject gives the formula `(max(l, r) - min(l, r)) * (1 - c)` where `l`, `c` and `r` are the
left neighbor, the cell and the right neighbor. `max - min` of two bits is 1 exactly when one of
them is set, i.e. when the two outer neighbors hold exactly one live cell:

```text
MUL(EQ(COMPTER(G2*), 1), SUB(1, COMPTER(G0)))
```

### Rule 30: telling left from right

`next = left XOR (center OR right)` is not symmetric, so it needs one-cell neighborhoods:

```xml
<neighborhoods>
  <neighborhood name="G11">{(-1)}</neighborhood>
  <neighborhood name="G12">{(1)}</neighborhood>
</neighborhoods>
<rule>EQ(ADD(COMPTER(G11), OU(COMPTER(G0), COMPTER(G12))), 1)</rule>
```

`EQ(ADD(a, b), 1)` is the exclusive or of two bits.

### Brian's Brain: three states

```text
SI(EQ(ETAT(), 1), 2,
   SI(EQ(ETAT(), 2), 0,
      EQ(COMPTER(G8*), 2)))
```

A firing cell (1) becomes refractory (2), a refractory cell becomes ready (0), and a ready cell
fires when exactly two of its neighbors are firing.

## Error messages

A rule that does not parse is rejected when its file is read. The message says what is wrong and
shows where:

```text
SI expects 3 argument(s) but got 2 (line 1, column 1)
    SI(EQ(COMPTER(G0), 1), 0)
    ^
```

```text
unknown neighborhood 'G26' for a 2-dimensional grid (available: G0, G4, G4*, G8, G8*) (line 1, column 9)
    COMPTER(G26)
            ^
```

```text
neighborhood G8 cannot be used as a value; count its cells with COMPTER(G8) (line 1, column 8)
    ADD(1, G8)
           ^
```
