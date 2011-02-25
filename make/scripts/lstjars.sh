#! /bin/sh

THISDIR=$(pwd)

BUILDDIR=$1
shift
BUILDDIR_GLUEGEN=$1
shift
if [ -z "$BUILDDIR" -o -z "$BUILDDIR_GLUEGEN" ] ; then 
    echo "usage $0 <BUILDDIR-JOGL> <BUILDDIR-GLUEGEN> [-pack200]"
    exit 1
fi

STATDIR=$BUILDDIR-stats

pack200=0
if [ "$1" = "-pack200" ] ; then
    pack200=1
fi

function report() {
    #ls -1 -s --block-size=1024 $*
    #ls -1 -s --block-size=1024 $* | awk ' BEGIN { sum=0 ; } { sum=sum+$1; } END { printf("%d Total\n", sum); }'
    du -ksc $*
}

OSS=x11
ARCH=linux-amd64

function listdeployment() {
    JAR_SUFFIX=$1
    shift

    echo JOGL Deployment Payload for $JAR_SUFFIX
    echo

    echo JOGL ES1 NEWT CORE
    report gluegen-rt.$JAR_SUFFIX nativewindow.all-noawt.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.egl.$JAR_SUFFIX jogl.gles1.$JAR_SUFFIX newt.all-noawt.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_es1.so.gz libnewt.so.gz
    echo

    echo JOGL ES2 NEWT CORE
    report gluegen-rt.$JAR_SUFFIX nativewindow.all-noawt.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.egl.$JAR_SUFFIX jogl.gles2.$JAR_SUFFIX newt.all-noawt.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_es2.so.gz libnewt.so.gz
    echo

    echo JOGL ES2 NEWT CORE FIXED
    report gluegen-rt.$JAR_SUFFIX nativewindow.all-noawt.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.egl.$JAR_SUFFIX jogl.gles2.$JAR_SUFFIX jogl.util.fixedfuncemu.$JAR_SUFFIX newt.all-noawt.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_es2.so.gz libnewt.so.gz
    echo

    echo JOGL GL2ES12 NEWT 
    report gluegen-rt.$JAR_SUFFIX nativewindow.all-noawt.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.os.$OSS.$JAR_SUFFIX jogl.gl2es12.$JAR_SUFFIX newt.all-noawt.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_gl2es12.so.gz libnewt.so.gz libnativewindow_$OSS.so.gz
    echo

    echo JOGL GL2 NEWT 
    report gluegen-rt.$JAR_SUFFIX nativewindow.all-noawt.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.os.$OSS.$JAR_SUFFIX jogl.gldesktop.$JAR_SUFFIX newt.all-noawt.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_desktop.so.gz libnewt.so.gz libnativewindow_$OSS.so.gz
    echo

    echo JOGL GL2 AWT
    report gluegen-rt.$JAR_SUFFIX nativewindow.all.$JAR_SUFFIX jogl.core.$JAR_SUFFIX jogl.util.$JAR_SUFFIX jogl.os.$OSS.$JAR_SUFFIX jogl.gldesktop.$JAR_SUFFIX jogl.awt.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_desktop.so.gz libnativewindow_$OSS.so.gz libnativewindow_awt.so.gz
    echo

    echo JOGL ALL AWT
    report gluegen-rt.$JAR_SUFFIX nativewindow.all.$JAR_SUFFIX jogl.all.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_desktop.so.gz libnativewindow_$OSS.so.gz libnativewindow_awt.so.gz
    echo

    echo JOGL ALL No AWT
    report gluegen-rt.$JAR_SUFFIX nativewindow.all-noawt.$JAR_SUFFIX jogl.all-noawt.$JAR_SUFFIX newt.all-noawt.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_desktop.so.gz libnativewindow_$OSS.so.gz libnewt.so.gz
    echo

    echo JOGL GLU
    report jogl.glu.*$JAR_SUFFIX
    echo

    echo JOGL EVERYTHING
    report *.all.$JAR_SUFFIX libgluegen-rt.so.gz libnativewindow_$OSS.so.gz libnativewindow_awt.so.gz libjogl_desktop.so.gz  libjogl_es1.so.gz  libjogl_es2.so.gz  libnewt.so.gz
    echo
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
cp -a $BUILDDIR_GLUEGEN/gluegen-rt-natives-linux-i586.jar $STATDIR
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

mv jogl.test.lst *-noawt.lst *.all*.lst nope/

mv jogl.gl2es12.*.lst jogl.gldesktop.*.lst nope/
echo duplicates - w/o gl2es12.* gldesktop.*
echo
sort jogl*.lst | uniq -d
mv nope/* .

mv jogl.test.lst *.all*.lst gluegen-gl.lst nope/
cat *.lst | sort -u > allparts.lst
mv nope/* .
cat *.all.lst gluegen-rt.lst   | sort -u > allall.lst

echo all vs allparts delta
echo
diff -Nur allparts.lst allall.lst

listdeployment jar

if [ $pack200 -eq 1 ] ; then
    for i in *.jar ; do
        fname=$i
        bname=$(basename $fname .jar)
        echo pack200 $bname.pack.gz $fname
        pack200 $bname.pack.gz $fname
    done
    listdeployment pack.gz
fi

