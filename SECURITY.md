# Security policy

## Supported versions

Only the latest release receives fixes.

## What is in scope

Cellula is a desktop application. It opens files chosen by its user and never listens on the
network. The one place where it handles data that may come from someone else is the reading of
automaton definition files, which are XML.

That parser is configured defensively:

- document type declarations (`<!DOCTYPE ...>`) are refused, which rules out XML external entity
  (XXE) and entity expansion ("billion laughs") attacks;
- XInclude and external schema access are disabled;
- the size of a grid is capped, so a hostile file cannot exhaust the memory by asking for an
  enormous grid, and the nesting depth of a rule is capped as well.

A definition file that makes the application read another file, reach the network, or crash
instead of reporting an error is a bug worth reporting.

## Reporting a vulnerability

Please do not open a public issue. Use GitHub's private vulnerability reporting ("Security" tab,
"Report a vulnerability") or contact the maintainer directly. You should get an answer within a
week.
