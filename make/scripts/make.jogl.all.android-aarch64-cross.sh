if [ -e ${SDIR}/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh ] ; then
    . ${SDIR}/../../../gluegen/make/scripts/setenv-build-jogamp-x86_64.sh
fi

LOGF=make.jogl.all.android-aarch64-cross.log
rm -f ${LOGF}

export ANDROID_HOME=/opt-linux-x86_64/android-sdk-linux_x86_64
export ANDROID_HOST_TAG=linux-x86_64
export ANDROID_ABI=arm64-v8a

if [ -e ${SDIR}/../../../gluegen/make/scripts/setenv-android-tools.sh ] ; then
    . ${SDIR}/../../../gluegen/make/scripts/setenv-android-tools.sh >> $LOGF 2>&1
else
    echo "${SDIR}/../../../setenv-android-tools.sh doesn't exist!" 2>&1 | tee -a ${LOGF}
    exit 1
fi

export GLUEGEN_CPPTASKS_FILE=${SDIR}/../../../gluegen/make/lib/gluegen-cpptasks-android-aarch64.xml
export PATH_VANILLA=$PATH
export PATH=${ANDROID_TOOLCHAIN_ROOT}/${ANDROID_TOOLCHAIN_NAME}/bin:${ANDROID_TOOLCHAIN_ROOT}/bin:${ANDROID_HOME}/platform-tools:${ANDROID_BUILDTOOLS_ROOT}:${PATH}
echo PATH ${PATH} 2>&1 | tee -a ${LOGF}
echo clang `which clang` 2>&1 | tee -a ${LOGF}

export NODE_LABEL=.

export HOST_UID=jogamp
# jogamp02 - 10.1.0.122
export HOST_IP=10.1.0.122
export HOST_RSYNC_ROOT=PROJECTS/JogAmp

export TARGET_UID=jogamp
export TARGET_IP=panda02
#export TARGET_IP=jautab03
#export TARGET_IP=jauphone04
export TARGET_ADB_PORT=5555
# needs executable bit (probably su)
export TARGET_ROOT=/data/projects
export TARGET_ANT_HOME=/usr/share/ant

#export JUNIT_DISABLED="true"
#export JUNIT_RUN_ARG0="-Dnewt.test.Screen.disableScreenMode"

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

#BUILD_ARCHIVE=true \
ant \
    -Drootrel.build=build-android-aarch64 \
    -Dgcc.compat.compiler=clang \
    $* 2>&1 | tee -a ${LOGF}
