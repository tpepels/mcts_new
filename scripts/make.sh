#!/bin/sh
dirs="amazons atarigo gomoku hex TicTacToe breakthrough experiments framework mcts"

for d in $dirs
do
  mkdir -p build/$d
  cp -r src/$d/* build/$d
done

files=`find build -name *.java`
javac -cp build $files