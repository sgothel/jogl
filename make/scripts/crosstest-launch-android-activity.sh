#! /bin/sh

#adb uninstall com.jogamp.common
#adb install ../../gluegen/build-android-armv7/gluegen-rt.apk

adb uninstall javax.media.opengl
adb install ../build-android-armv7/jar/jogl.all-android.apk 

adb shell "setprop log.redirect-stdio true ; setprop log.redirect-stderr true ; \
           am start -a android.intent.action.MAIN -n javax.media.opengl/jogamp.newt.driver.android.NewtVersionActivity"

#adb uninstall com.jogamp.android.launcher
#adb install ../build-android-armv7/android/jar/jogllauncher.apk 

#adb shell "setprop log.redirect-stdio true ; setprop log.redirect-stderr true ; \
#           am start -a android.intent.action.MAIN -n com.jogamp.android.launcher/com.jogamp.android.launcher.NEWTLauncherVersionActivity"

