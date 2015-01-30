sdir=`dirname $0`

$sdir/adb-uninstall-all.sh $*
$sdir/adb-install-all-arm64-v8a.sh $*

