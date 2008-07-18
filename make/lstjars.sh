#! /bin/sh

THISDIR=$(pwd)

function report() {
    #ls -1 -s --block-size=1024 $*
    #ls -1 -s --block-size=1024 $* | awk ' BEGIN { sum=0 ; } { sum=sum+$1; } END { printf("%d Total\n", sum); }'
    du -ksc $*
}

cd $THISDIR/../build/obj
mkdir -p tmp
cd tmp

rm -f *.so *.so.gz
cp ../*.so .
for i in *.so ; do
    gzip $i
done
echo Native Libraries
report *.gz
echo

cd $THISDIR/../build

rm -f *.lst

for i in *.jar ; do
    fname=$i
    bname=$(basename $fname .jar)
    echo list $fname to $bname.lst
    jar tf $fname | grep class | sort > $bname.lst
done

mv jogl.all.lst jogl.all.lst.nope

echo duplicates
echo
sort *.lst | uniq -d

cat *.lst | sort > jogl.allparts.lst
mv jogl.all.lst.nope jogl.all.lst

echo jogl.all bs jogl.allparts delta
echo
diff -Nur jogl.allparts.lst jogl.all.lst

echo JOGL ES1 NEWT CORE
report jogl.core.jar jogl.egl.jar jogl.gles1.jar newt.jar obj/tmp/libjogl_es1.so.gz obj/tmp/libnewt.so.gz
echo

echo JOGL ES2 NEWT CORE
report jogl.core.jar jogl.egl.jar jogl.gles2.jar newt.jar obj/tmp/libjogl_es2.so.gz obj/tmp/libnewt.so.gz
echo

echo JOGL GL2 NEWT CORE no AWT
report jogl.core.jar jogl.oswin.jar jogl.gl2.jar newt.jar obj/tmp/libjogl_gl2.so.gz obj/tmp/libnewt.so.gz
echo

echo JOGL GL2 OSWIN no AWT
report jogl.core.jar jogl.oswin.jar jogl.gl2.jar obj/tmp/libjogl_gl2.so.gz
echo

echo JOGL GL2 OSWIN with AWT
report jogl.core.jar jogl.oswin.jar jogl.gl2.jar jogl.awt.jar obj/tmp/libjogl_gl2.so.gz obj/tmp/libjogl_awt.so.gz
echo

echo JOGL EVERYTHING
report jogl.all.jar
echo
