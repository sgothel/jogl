#!/bin/sh

fatonly=0
if [ "$1" = "-fatonly" ] ; then
  fatonly=1
  shift
fi

adb $* install ../build-android-x86_64/jar/jogl-demos-fat-android-x86_64.apk

if [ $fatonly -eq 1 ] ; then
  return 0
fi

adb $* install ../../gluegen/build-android-x86_64/jogamp-android-launcher.apk
adb $* install ../../gluegen/build-android-x86_64/gluegen-rt-android-x86_64.apk
adb $* install ../../joal/build-android-x86_64/jar/joal-android-x86_64.apk
adb $* install ../../joal/build-android-x86_64/jar/joal-test-android.apk
adb $* install ../build-android-x86_64/jar/jogl-all-android-x86_64.apk
adb $* install ../build-android-x86_64/jar/jogl-demos-android.apk
