#!/bin/sh
dirs="amazons atarigo gomoku hex TicTacToe breakthrough experiments framework mcts"
class=`shift`

CP=".:lib/*"
for d in $dirs
do
  CP="$CP:build/$d"
done

CP="$CP:build/lib/commons-math3-3.6.1.jar:build/lib/xchart-3.6.0.jar"

echo java -Xmx2048m -Xms512m -XX:+UseSerialGC -classpath build $class $@

java -Xmx2048m -Xms512m -XX:+UseSerialGC -classpath "lib/*:build"  $class $@
