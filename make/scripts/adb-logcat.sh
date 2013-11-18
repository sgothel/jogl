sdir=`dirname $0`

adb $* logcat -c
adb $* logcat 2>&1 | tee adb-logcat.log
