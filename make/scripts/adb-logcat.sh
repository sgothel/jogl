sdir=`dirname $0`

#adb $* shell stop
#adb $* shell setprop log.redirect-stdio true
#adb $* shell setprop log.redirect-stderr true
#adb $* shell start
adb $* logcat -c
adb $* logcat 2>&1 | tee adb-logcat.log
