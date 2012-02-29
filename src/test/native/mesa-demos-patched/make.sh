
THISDIR=`pwd`

#eglut/eglut_x11.c \
#eglut/eglut_screen.c \

gcc -I$THISDIR -I$THISDIR/eglut -o es2gears \
eglut/eglut.c \
eglut/eglut_x11.c \
es2gears.c \
-lX11 \
-lEGL \
-lGLESv2 \
