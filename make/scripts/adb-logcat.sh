sdir=`dirname $0`

adb $* logcat -c
adb $* logcat
