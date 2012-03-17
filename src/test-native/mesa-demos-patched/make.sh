
THISDIR=`pwd`

#eglut/eglut_x11.c \
#eglut/eglut_screen.c \

#DEMO_C=es2gears.c
#DEMO_C=es2redsquare.c
DEMO_C=$1

gcc -I$THISDIR -I$THISDIR/eglut -o `basename $DEMO_C .c` \
eglut/eglut.c \
eglut/eglut_x11.c \
$DEMO_C \
-lX11 \
-lEGL \
-lGLESv2 \
