#! /bin/sh

cd ../build

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
du -ksc jogl.core.jar jogl.egl.jar jogl.gles1.jar newt.jar
echo

echo JOGL ES2 NEWT CORE
du -ksc jogl.core.jar jogl.egl.jar jogl.gles2.jar newt.jar
echo

echo JOGL GL2 OSWIN CORE no AWT
du -ksc jogl.core.jar jogl.oswin.jar jogl.gl2.jar newt.jar
echo

echo JOGL GL2 OSWIN with AWT
du -ksc jogl.core.jar jogl.oswin.jar jogl.gl2.jar jogl.awt.jar 
echo

echo JOGL EVERYTHING
du -ksc jogl.all.jar
echo
