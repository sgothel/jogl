#! /bin/sh

THISDIR=$(pwd)
STATDIR=$THISDIR/../stats

BUILDDIR=$1
shift
BUILDDIR_GLUEGEN=$1
shift
if [ -z "$BUILDDIR" -o -z "$BUILDDIR_GLUEGEN" ] ; then 
    echo "usage $0 <BUILDDIR-JOGL> <BUILDDIR-GLUEGEN>"
    exit 1
fi

skippack200=0
if [ "$1" = "-skippack200" ] ; then
    skippack200=1
fi

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
cp -a $BUILDDIR_GLUEGEN/gluegen-rt.jar $STATDIR
cp -a $BUILDDIR_GLUEGEN/gluegen-rt-cdc.jar $STATDIR
cp -a $BUILDDIR_GLUEGEN/gluegen-rt-natives-linux-i586.jar $STATDIR
cp -a $BUILDDIR_GLUEGEN/gluegen-rt-natives-linux-i586-cdc.jar $STATDIR
cp -a $BUILDDIR_GLUEGEN/obj/libgluegen-rt.so $STATDIR

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

mv *.cdc.lst *.all.lst nope/

mv jogl.gl2es12.*.lst jogl.gl2.*.lst nope/
echo duplicates - w/o gl2es12.* gl2.*
echo
sort jogl*.lst | uniq -d
mv nope/* .

mv *.cdc.lst *.all.lst nope/
cat *.lst | sort -u > allparts.lst
mv nope/* .
cat *.all.lst   | sort -u > allall.lst
cat jogl.all.cdc.lst newt.all.cdc.lst nativewindow.core.lst | sort -u > allcdc.lst

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
report gluegen-rt.$JAR_SUFFIX nativewindow.core.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.egl.$JAR_SUFFIX jogl.gles1.$JAR_SUFFIX newt.core.$JAR_SUFFIX newt.ogl.$JAR_SUFFIX libgluegen-rt.so.gz libnativewindow_jvm.so.gz libjogl_es1.so.gz libnewt.so.gz
echo

echo JOGL ES2 NEWT CORE
report gluegen-rt.$JAR_SUFFIX nativewindow.core.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.egl.$JAR_SUFFIX jogl.gles2.$JAR_SUFFIX newt.core.$JAR_SUFFIX newt.ogl.$JAR_SUFFIX libgluegen-rt.so.gz libnativewindow_jvm.so.gz libjogl_es2.so.gz libnewt.so.gz
echo

echo JOGL ES2 NEWT CORE FIXED
report gluegen-rt.$JAR_SUFFIX nativewindow.core.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.egl.$JAR_SUFFIX jogl.gles2.$JAR_SUFFIX jogl.util.fixedfuncemu.$JAR_SUFFIX newt.core.$JAR_SUFFIX newt.ogl.$JAR_SUFFIX libgluegen-rt.so.gz libnativewindow_jvm.so.gz libjogl_es2.so.gz libnewt.so.gz
echo

echo JOGL GL2ES12 NEWT 
report gluegen-rt.$JAR_SUFFIX nativewindow.core.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.gl2es12.$OSS.$JAR_SUFFIX newt.core.$JAR_SUFFIX newt.ogl.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_gl2es12.so.gz libnewt.so.gz libnativewindow_$OSS.so.gz libnativewindow_jvm.so.gz
echo

echo JOGL GL2 NEWT 
report gluegen-rt.$JAR_SUFFIX nativewindow.core.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.gl2.$OSS.$JAR_SUFFIX newt.core.$JAR_SUFFIX newt.ogl.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_gl2.so.gz libnewt.so.gz libnativewindow_$OSS.so.gz libnativewindow_jvm.so.gz
echo

echo JOGL GL2 AWT
report gluegen-rt.$JAR_SUFFIX nativewindow.core.$JAR_SUFFIX nativewindow.awt.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.gl2.$OSS.$JAR_SUFFIX jogl.awt.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_gl2.so.gz libnativewindow_$OSS.so.gz libnativewindow_awt.so.gz libnativewindow_jvm.so.gz
echo

echo JOGL ALL
report gluegen-rt.$JAR_SUFFIX nativewindow.all.$JAR_SUFFIX jogl.all.$JAR_SUFFIX newt.all.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_gl2.so.gz libnativewindow_$OSS.so.gz libnativewindow_awt.so.gz libnativewindow_jvm.so.gz libnewt.so.gz
echo

echo JOGL CDCFP DESKTOP
report gluegen-rt-cdc.$JAR_SUFFIX nativewindow.core.$JAR_SUFFIX jogl.all.cdc.$JAR_SUFFIX newt.all.cdc.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_gl2es12.so.gz libnativewindow_$OSS.so.gz libnativewindow_jvm.so.gz libnewt.so.gz
echo

echo JOGL CDCFP ES1 MOBILE
report gluegen-rt-cdc.$JAR_SUFFIX jogl.core.cdc.$JAR_SUFFIX jogl.egl.cdc.$JAR_SUFFIX jogl.gles1.cdc.$JAR_SUFFIX jogl.util.cdc.$JAR_SUFFIX nativewindow.core.cdc.$JAR_SUFFIX newt.broadcomegl.cdc.$JAR_SUFFIX newt.core.cdc.$JAR_SUFFIX newt.ogl.cdc.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_es1.so.gz libnativewindow_jvm.so.gz libnewt.so.gz
echo

echo JOGL CDCFP ES2 MOBILE
report gluegen-rt-cdc.$JAR_SUFFIX jogl.core.cdc.$JAR_SUFFIX jogl.egl.cdc.$JAR_SUFFIX jogl.gles2.cdc.$JAR_SUFFIX jogl.util.cdc.$JAR_SUFFIX nativewindow.core.cdc.$JAR_SUFFIX newt.broadcomegl.cdc.$JAR_SUFFIX newt.core.cdc.$JAR_SUFFIX newt.ogl.cdc.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_es2.so.gz libnativewindow_jvm.so.gz libnewt.so.gz
echo

echo JOGL GLU
report jogl.glu.*pack.gz
echo

echo JOGL EVERYTHING
report *.all.pack.gz
echo
