#! /bin/bash

find . -name "*.aux" -type f -delete
find . -name "*.bbl" -type f -delete
find . -name "*.blg" -type f -delete
find . -name "*.fdb_latexmk" -type f -delete
find . -name "*.fls" -type f -delete
find . -name "*.lof" -type f -delete
find . -name "*.log" -type f -delete
find . -name "*.lot" -type f -delete
find . -name "*.out" -type f -delete
find . -name "*.synctex.gz" -type f -delete
find . -name "*.toc" -type f -delete
find . -name "*~" -type f -delete

echo "Clean :)"
