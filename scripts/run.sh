#!/bin/sh
dirs="amazons atarigo gomoku hex TicTacToe breakthrough experiments framework mcts"
class=`shift`

CP="."
for d in $dirs
do
  CP="$CP:build/$d"
done

java -Xmx2048m -Xms512m -XX:+UseSerialGC -cp build $class $@


