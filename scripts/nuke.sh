#!/bin/sh

if [ "$1" = "" ]
then
echo "Woah! No arguments.. dangerous, exiting.."
exit
fi

if [ "$1" = "." ]
then
echo "Woah! '.', really? Exiting... "
exit
fi

if [ ! -d $1 ]
then
echo "Woah! ... not a directory? Exiting.."
exit
fi

rep=`echo "$1" | sed -e 's/^scratch/####/'`

if [ "$1" = "$rep" ]
then
echo "Woah! ... not a scratch directory? Exiting.."
exit
fi

rm -rf $1 && mkdir $1