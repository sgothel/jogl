#! /bin/sh

THISDIR=$(pwd)
STATDIR=$THISDIR/../stats

BUILDDIR=$1
shift
if [ -z "$BUILDDIR" ] ; then 
    echo "usage $0 <BUILDDIR>"
    exit 1
fi

skippack200=0
if [ "$1" = "-skippack200" ] ; then
    skippack200=1
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
    echo list $fname to $bname.lst
    jar tf $fname | grep class | sort > $bname.lst
done

rm -rf nope
mkdir -p nope

mv *.cdcfp.lst *.all.lst nope/

mv jogl.gl2es12.*.lst jogl.gl2.*.lst nope/
echo duplicates - w/o gl2es12.* gl2.*
echo
sort jogl*.lst | uniq -d
mv nope/* .

mv *.cdcfp.lst *.all.lst nope/
cat *.lst | sort -u > allparts.lst
mv nope/* .
cat *.all.lst   | sort -u > allall.lst
cat jogl.cdcfp.lst newt.cdcfp.lst nativewindow.core.lst | sort -u > allcdcfp.lst

echo all vs allparts delta
echo
diff -Nur allparts.lst allall.lst

if [ $skippack200 -eq 0 ] ; then
    for i in *.jar ; do
        fname=$i
        bname=$(basename $fname .jar)
        echo pack200 $bname.pack.gz $fname
        pack200 $bname.pack.gz $fname
    done
    JAR_SUFFIX=pack.gz
else
    JAR_SUFFIX=jar
fi

OSS=x11

echo JOGL ES1 NEWT CORE
report nativewindow.core.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.egl.$JAR_SUFFIX jogl.gles1.$JAR_SUFFIX newt.core.$JAR_SUFFIX newt.ogl.$JAR_SUFFIX libjogl_es1.so.gz libnewt.so.gz
echo

echo JOGL ES2 NEWT CORE
report nativewindow.core.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.egl.$JAR_SUFFIX jogl.gles2.$JAR_SUFFIX newt.core.$JAR_SUFFIX newt.ogl.$JAR_SUFFIX libjogl_es2.so.gz libnewt.so.gz
echo

echo JOGL ES2 NEWT CORE FIXED
report nativewindow.core.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.egl.$JAR_SUFFIX jogl.gles2.$JAR_SUFFIX jogl.util.fixedfuncemu.$JAR_SUFFIX newt.core.$JAR_SUFFIX newt.ogl.$JAR_SUFFIX libjogl_es2.so.gz libnewt.so.gz
echo

echo JOGL GL2ES12 NEWT 
report nativewindow.core.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.gl2es12.$OSS.$JAR_SUFFIX newt.core.$JAR_SUFFIX newt.ogl.$JAR_SUFFIX libjogl_gl2es12.so.gz libnewt.so.gz libnativewindow_$OSS.so.gz libnativewindow_jvm.so.gz
echo

echo JOGL GL2 NEWT 
report nativewindow.core.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.gl2.$OSS.$JAR_SUFFIX newt.core.$JAR_SUFFIX newt.ogl.$JAR_SUFFIX libjogl_gl2.so.gz libnewt.so.gz libnativewindow_$OSS.so.gz libnativewindow_jvm.so.gz
echo

echo JOGL GL2 AWT
report nativewindow.core.$JAR_SUFFIX nativewindow.awt.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.gl2.$OSS.$JAR_SUFFIX jogl.awt.$JAR_SUFFIX libjogl_gl2.so.gz libjogl_awt.so.gz libnativewindow_$OSS.so.gz libnativewindow_awt.so.gz libnativewindow_jvm.so.gz
echo

echo JOGL ALL
report nativewindow.all.$JAR_SUFFIX jogl.all.$JAR_SUFFIX newt.all.$JAR_SUFFIX libjogl_gl2.so.gz libjogl_awt.so.gz libnativewindow_$OSS.so.gz libnativewindow_awt.so.gz libnativewindow_jvm.so.gz libnewt.so.gz
echo

echo JOGL CDCFP
report nativewindow.core.$JAR_SUFFIX jogl.cdcfp.$JAR_SUFFIX newt.cdcfp.$JAR_SUFFIX libjogl_gl2es12.so.gz libnativewindow_$OSS.so.gz libnativewindow_jvm.so.gz libnewt.so.gz
echo

echo JOGL GLU
report jogl.glu.*pack.gz
echo

echo JOGL EVERYTHING
report *.all.pack.gz
echo
