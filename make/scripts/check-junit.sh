#! /bin/bash

builddir=$1
shift

function checkresult() {
    resdir=$1
    shift
    if [ -e $builddir/test/$resdir ] ; then
        echo
        echo Results of $builddir/test/$resdir
        echo
        echo number of junit classes
        grep failures $builddir/test/$resdir/* | wc
        echo
        echo number of passed junit classes - failures
        grep failures $builddir/test/$resdir/* | grep "failures=\"0\"" | wc
        echo
        echo number of passed junit classes - errors
        grep failures $builddir/test/$resdir/* | grep "errors=\"0\"" | wc
        echo
        echo number of failed junit classes - failures
        grep failures $builddir/test/$resdir/* | grep -v "failures=\"0\"" | wc
        echo
        echo number of failed junit classes - errors
        grep failures $builddir/test/$resdir/* | grep -v "errors=\"0\"" | wc
        echo
        echo failed junit classes - failures
        grep failures $builddir/test/$resdir/* | grep -v "failures=\"0\""
        echo
        echo failed junit classes - errors
        grep failures $builddir/test/$resdir/* | grep -v "errors=\"0\""
        echo
    fi
}

checkresult results
checkresult results-x32
