#! /bin/bash

build_dir=$1
shift

if [ -z "$build_dir" -o ! -e "${build_dir}" ] ; then
    echo "Usage $0 build_dir [test_class]"
    exit 1
fi

build_dir_base=`basename ${build_dir}`

if [ -z "$1" ] ; then
    test_class=com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextWithJTabbedPaneAWT
else
    test_class=$1
    shift
fi

TST_CLASSPATH=".:../../gluegen/${build_dir_base}/gluegen-rt.jar:../../joal/${build_dir_base}/jar/joal.jar:${build_dir}/jar/jogl-all.jar:${build_dir}/jar/jogl-test.jar:${SWT_CLASSPATH}:../../gluegen/make/lib/junit.jar:../../gluegen/make/lib/semantic-versioning/semver.jar:../../gluegen/${build_dir_base}/gluegen-test-util.jar"

set -o pipefail

ulimit -c unlimited

do_test() {
    OK=1
    java \
        -cp ${TST_CLASSPATH} \
        -Djunit.run.arg0=dummy -Djunit.run.arg1=dummy -Djnlp.no.jvm.data.model.set=true \
        -Djava.library.path=../../gluegen/${build_dir_base}/obj:${build_dir}/nativewindow/obj:${build_dir}/jogl/obj:${build_dir}/newt/obj:${build_dir}/test/build/obj \
        --enable-native-access=ALL-UNNAMED \
        --add-opens java.desktop/sun.awt=ALL-UNNAMED --add-opens java.desktop/sun.awt.windows=ALL-UNNAMED --add-opens java.desktop/sun.java2d=ALL-UNNAMED \
        org.junit.runner.JUnitCore ${test_class} \
        && OK=0

    return $OK
}

echo logfile ${test_class}.log 2>&1 | tee ${test_class}.log
which java 2>&1 | tee -a ${test_class}.log
java -version 2>&1 | tee -a ${test_class}.log
echo "Classpath: ${TST_CLASSPATH}" | tee -a ${test_class}.log

while true ; do 
    do_test 2>&1 | tee -a ${test_class}.log
    efile=`find . -maxdepth 1 -a -name hs_err\*.log`
    if [ -e "$efile" ] ; then
        echo do_test error 
        exit 1
    fi
    echo do_test OK 
done
echo do_test out 

