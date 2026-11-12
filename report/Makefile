# Builds the report. Requires a LaTeX distribution providing latexmk.
#
#   make          build report.pdf
#   make watch    rebuild on every change, with a live preview
#   make clean    remove the intermediate files

LATEXMK = latexmk -pdf -interaction=nonstopmode -halt-on-error -output-directory=build

.PHONY: all watch clean

all: report.pdf

report.pdf: report.tex $(wildcard sections/*.tex) $(wildcard ../docs/images/*.png)
	$(LATEXMK) report.tex
	cp build/report.pdf report.pdf

watch:
	$(LATEXMK) -pvc report.tex

clean:
	rm -rf build
