#! /bin/sh

echo EDT START
grep "EDT run() START" $1 | wc

echo EDT END
grep "EDT run() END" $1 | wc

echo EDT EXIT
grep "EDT run() EXIT" $1 | wc

