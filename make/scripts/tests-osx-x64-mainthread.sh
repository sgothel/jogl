
export CLASSPATH=.:../../gluegen/make/../build-macosx/gluegen-rt.jar:../build-macosx/jar/jogl-all-noawt.jar:../build-macosx/jar/jogl-test.jar:../build-macosx/../make/lib/swt/cocoa-macosx-x86_64/swt-debug.jar:../../gluegen/make/../make/lib/junit.jar:/opt-share/apache-ant/lib/ant.jar:/opt-share/apache-ant/lib/ant-junit.jar:../build-macosx/jar/atomic/jogl-swt.jar:../build-macosx/jar/jogl-test.jar

/usr/bin/java -d64 -XstartOnFirstThread -Djava.awt.headless=true  com.jogamp.opengl.test.junit.jogl.swt.TestSWTJOGLGLCanvas01GLn
#/usr/bin/java -d64 -XstartOnFirstThread -Djava.awt.headless=false com.jogamp.opengl.test.junit.jogl.swt.TestSWTJOGLGLCanvas01GLn

#
# Not working!
#/usr/bin/java -d64                      -Djava.awt.headless=true  com.jogamp.opengl.test.junit.jogl.swt.TestSWTJOGLGLCanvas01GLn
#/usr/bin/java -d64                      -Djava.awt.headless=false com.jogamp.opengl.test.junit.jogl.swt.TestSWTJOGLGLCanvas01GLn

