#! /bin/bash

# export LD_LIBRARY_PATH=/Users/jogamp/projects/JogAmp/gluegen/build/obj:/Users/jogamp/projects/JogAmp/jogl/build/lib
# export DYLD_LIBRARY_PATH=$LD_LIBRARY_PATH

#rm -rf natives
#mkdir -p natives/macosx-universal
#cp -av /Users/jogamp/projects/JogAmp/gluegen/build/obj/libgluegen_rt.so natives/macosx-universal/
#cp -av natives/macosx-universal/libgluegen_rt.so natives/macosx-universal/libgluegen_rt.dylib
#cp -av /Users/jogamp/projects/JogAmp/jogl/build/lib/*dylib natives/macosx-universal/

# ./Bug1398macOSContextOpsOnMainThread /Users/jogamp/projects/JogAmp/gluegen/build/obj:/Users/jogamp/projects/JogAmp/jogl/build/lib/lib
# ./Bug1398macOSContextOpsOnMainThread /Library/Java/JavaVirtualMachines/jdk1.8.0_192.jdk/Contents/Home/jre/lib/server/libjvm.dylib

./Bug1398macOSContextOpsOnMainThread /Library/Java/JavaVirtualMachines/jdk1.8.0_192.jdk/Contents/MacOS/libjli.dylib
# ./Bug1398macOSContextOpsOnMainThread /Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/MacOS/libjli.dylib
