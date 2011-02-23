#! /bin/bash

dest0=../doc/Implementation
dest=../doc/Implementation/runtime-properties-temp

rm -rf $dest
mkdir $dest

function cleanup() {
    tfile=$1
    shift
    ffile=$1
    shift
    domain=$1
    shift

    sed -e 's/^.*(\"//g' -i $tfile
    sed -e 's/\".*$//g' -i $tfile
    sed -e "s/^/$domain/g" -i $tfile

    sort -u $tfile >> $ffile
    rm -f $tfile
}

grep -hRI "Debug\.debug" ../../gluegen/src/java/com/jogamp | sort -u > $dest/gluegen-rt.debug.tmp1.txt 
grep -hRI "System.getProperty(\"jogamp" ../../gluegen/src/java/com/jogamp | sort -u > $dest/gluegen-rt.debug.tmp2.txt
cleanup $dest/gluegen-rt.debug.tmp1.txt $dest/gluegen-rt.debug.txt jogamp.debug.
cleanup $dest/gluegen-rt.debug.tmp2.txt $dest/gluegen-rt.debug.txt

grep -hRI -e "Debug\.isPropertyDefined" -e "Debug\.get" ../../gluegen/src/java/com/jogamp | sort -u > $dest/gluegen-rt.debug.ipd.tmp1.txt
cleanup $dest/gluegen-rt.debug.ipd.tmp1.txt $dest/gluegen-rt.ipd.debug.txt

grep -hRI "Debug\.debug" ../src/nativewindow | sort -u > $dest/nativewindow.debug.tmp1.txt
cleanup $dest/nativewindow.debug.tmp1.txt $dest/nativewindow.debug.txt nativewindow.debug.
grep -hRI -e "Debug\.isPropertyDefined" -e "Debug\.get" ../src/nativewindow | sort -u > $dest/nativewindow.debug.ipd.tmp1.txt
cleanup $dest/nativewindow.debug.ipd.tmp1.txt $dest/nativewindow.ipd.debug.txt

grep -hRI "Debug\.debug" ../src/jogl | sort -u > $dest/jogl.debug.all.tmp1.txt
cleanup $dest/jogl.debug.all.tmp1.txt $dest/jogl.debug.all.txt jogl.debug. 
grep -hRI -e "Debug\.isPropertyDefined" -e "Debug\.get" ../src/jogl | sort -u > $dest/jogl.debug.ipd.tmp1.txt
cleanup $dest/jogl.debug.ipd.tmp1.txt $dest/jogl.ipd.debug.txt

grep -hRI "Debug\.debug" ../src/newt | sort -u > $dest/newt.debug.tmp1.txt
cleanup $dest/newt.debug.tmp1.txt $dest/newt.debug.txt newt.debug.
grep -hRI -e "Debug\.isPropertyDefined" -e "Debug\.get" ../src/newt | sort -u > $dest/newt.debug.ipd.tmp1.txt
cleanup $dest/newt.debug.ipd.tmp1.txt $dest/newt.ipd.debug.txt

function onefile() {
    for i in $dest/* ; do 
        echo $i 
        echo ----------------------------------------
        sed 's/^/    /g' $i 
        echo 
        echo 
        echo 
    done
}

onefile > $dest0/runtime-properties-new.txt

rm -rf $dest

