#/bin/zsh
dot -Tpng $1 -o "./outputs/"$1".png"
open ./outputs/$1.png