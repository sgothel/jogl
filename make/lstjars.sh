#! /bin/sh

THISDIR=$(pwd)
STATDIR=$THISDIR/../stats

BUILDDIR=$1
if [ -z "$BUILDDIR" ] ; then 
    echo "usage $0 <BUILDDIR>"
    exit 1
fi

idir=$BUILDDIR/jogl/gensrc/classes/javax/media/opengl


function report() {
    #ls -1 -s --block-size=1024 $*
    #ls -1 -s --block-size=1024 $* | awk ' BEGIN { sum=0 ; } { sum=sum+$1; } END { printf("%d Total\n", sum); }'
    du -ksc $*
}

rm -rf $STATDIR
mkdir -p $STATDIR
cp -a $BUILDDIR/nativewindow/obj/*.so $STATDIR
cp -a $BUILDDIR/jogl/obj/*.so $STATDIR
cp -a $BUILDDIR/newt/obj/*.so $STATDIR
cp -a $BUILDDIR/nativewindow/*.jar $STATDIR
cp -a $BUILDDIR/jogl/*.jar $STATDIR
cp -a $BUILDDIR/newt/*.jar $STATDIR

cd $STATDIR

for i in *.so ; do
    gzip $i
done

echo Native Libraries
report *.gz
echo

rm -f *.lst

for i in *.jar ; do
    fname=$i
    bname=$(basename $fname .jar)
    echo pack200 $bname.pack.gz $fname
    pack200 $bname.pack.gz $fname
    echo list $fname to $bname.lst
    jar tf $fname | grep class | sort > $bname.lst
done

rm -rf nope
mkdir -p nope

rm -f allparts.lst allall.lst

mv jogl.all.lst nope/

mv jogl.gl2es12.*.lst jogl.gl2.*.lst nope/
echo duplicates - w/o gl2es12.* gl2.*
echo
sort jogl*.lst | uniq -d
mv nope/* .

mv *.all.lst nope/
cat *.lst | sort -u > allparts.lst
mv nope/* .
cat *.all.lst | sort -u > allall.lst

echo all vs allparts delta
echo
diff -Nur allparts.lst allall.lst

OSS=x11

echo JOGL ES1 NEWT CORE
report nativewindow.core.pack.gz jogl.core.pack.gz jogl.util.pack.gz jogl.egl.pack.gz jogl.gles1.pack.gz newt.core.pack.gz newt.ogl.pack.gz libjogl_es1.so.gz libnewt.so.gz
echo

echo JOGL ES2 NEWT CORE
report nativewindow.core.pack.gz jogl.core.pack.gz jogl.util.pack.gz jogl.egl.pack.gz jogl.gles2.pack.gz newt.core.pack.gz newt.ogl.pack.gz libjogl_es2.so.gz libnewt.so.gz
echo

echo JOGL ES2 NEWT CORE FIXED
report nativewindow.core.pack.gz jogl.core.pack.gz jogl.util.pack.gz jogl.egl.pack.gz jogl.gles2.pack.gz jogl.util.fixedfuncemu.pack.gz newt.core.pack.gz newt.ogl.pack.gz libjogl_es2.so.gz libnewt.so.gz
echo

echo JOGL GL2ES12 NEWT 
report nativewindow.core.pack.gz jogl.core.pack.gz jogl.util.pack.gz jogl.gl2es12.$OSS.pack.gz newt.core.pack.gz newt.ogl.pack.gz libjogl_gl2es12.so.gz libnewt.so.gz
echo

echo JOGL GL2 NEWT 
report nativewindow.core.pack.gz jogl.core.pack.gz jogl.util.pack.gz jogl.gl2.$OSS.pack.gz newt.core.pack.gz newt.ogl.pack.gz libjogl_gl2.so.gz libnewt.so.gz
echo

echo JOGL GL2 AWT
report nativewindow.core.pack.gz nativewindow.awt.pack.gz jogl.core.pack.gz jogl.util.pack.gz jogl.gl2.$OSS.pack.gz jogl.awt.pack.gz libjogl_gl2.so.gz libjogl_awt.so.gz libnativewindow_$OSS.so.gz libnativewindow_awt.so.gz
echo

echo JOGL GLU
report jogl.glu.*pack.gz
echo

echo JOGL EVERYTHING
report *.all.pack.gz
echo
