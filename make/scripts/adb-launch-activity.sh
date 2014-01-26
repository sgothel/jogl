#! /bin/sh

#ANAME="com.jogamp.opengl.test/com.jogamp.opengl.test.android.NEWTGenericActivity"
#ANAME="com.jogamp.android.launcher/com.jogamp.android.launcher.NEWTLauncherActivity2"
#ANAME="com.jogamp.opengl.test/com.jogamp.opengl.test.android.NEWTGearsES2ActivityLauncher"
ANAME="com.jogamp.opengl.test/com.jogamp.opengl.test.android.MovieCubeActivityLauncher0"

adb $* shell "setprop log.redirect-stdio true ; setprop log.redirect-stderr true ; \
              am start -a android.intent.action.MAIN -n $ANAME"



