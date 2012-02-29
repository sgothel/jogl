#! /bin/sh

let i=0

while true ; do
 let i=$i+1
 echo TEST RUN $i
 ./es2gears -loops 1 -time 1000
done
 
