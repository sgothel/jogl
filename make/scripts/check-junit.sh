#! /bin/bash

builddir=$1
shift

echo number of junit classes
grep failures $builddir/test/results/* | wc
echo
echo number of passed junit classes - failures
grep failures $builddir/test/results/* | grep "failures=\"0\"" | wc
echo
echo number of passed junit classes - errors
grep failures $builddir/test/results/* | grep "errors=\"0\"" | wc
echo
echo number of failed junit classes - failures
grep failures $builddir/test/results/* | grep -v "failures=\"0\"" | wc
echo
echo number of failed junit classes - errors
grep failures $builddir/test/results/* | grep -v "errors=\"0\"" | wc
echo
echo failed junit classes - failures
grep failures $builddir/test/results/* | grep -v "failures=\"0\""
echo
echo failed junit classes - errors
grep failures $builddir/test/results/* | grep -v "errors=\"0\""
echo

