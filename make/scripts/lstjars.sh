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

    echo JOGL ALL
    report gluegen-rt.$JAR_SUFFIX jogl.all.$JAR_SUFFIX libgluegen-rt.so.gz libnativewindow_awt.so.gz libnativewindow_x11.so.gz libjogl_desktop.so.gz libnewt.so.gz
    echo

    echo JOGL ALL no AWT
    report gluegen-rt.$JAR_SUFFIX jogl.all-noawt.$JAR_SUFFIX libgluegen-rt.so.gz libnativewindow_x11.so.gz libjogl_desktop.so.gz libnewt.so.gz
    echo

    echo JOGL Android - mobile egl es1 es2
    report gluegen-rt.$JAR_SUFFIX jogl.all-android.$JAR_SUFFIX libgluegen-rt.so.gz libjogl_mobile.so.gz
    echo
}

rm -rf $STATDIR
mkdir -p $STATDIR
cp -a $BUILDDIR/lib/*.so $STATDIR
cp -a $BUILDDIR/jar/*    $STATDIR
cp -a $BUILDDIR_GLUEGEN/gluegen-rt.jar $STATDIR
cp -a $BUILDDIR_GLUEGEN/obj/libgluegen-rt.so $STATDIR

cd $STATDIR

rm -rf nope
mkdir -p nope/atomic
mv gluegen*jar *-natives*.jar nope/
mv atomic/gluegen-gl.jar nope/atomic

for i in *.so ; do
    gzip $i
done

echo Native Libraries
report *.gz
echo

rm -f *.lst

for i in *.jar atomic/*.jar ; do
    fname=$i
    bname=$(dirname $fname)/$(basename $fname .jar)
    echo list $fname to $bname.lst
    jar tf $fname | grep class | sort > $bname.lst
done

echo duplicates in atomics
echo
sort atomic/jogl*.lst | uniq -d

cat atomic/*.lst | sort -u > allparts.lst
mv nope/*jar .
cat jogl.all.lst gluegen-rt.lst  | sort -u > allall.lst

echo all vs allparts delta
echo
diff -Nur allparts.lst allall.lst

mv nope/* .

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

