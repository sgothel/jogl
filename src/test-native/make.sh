#! /bin/bash

gcc -o displayMultiple01 displayMultiple01.c -lX11 -lGL
gcc -o displayMultiple02 displayMultiple02.c -lX11 -lGL
gcc -o glExtensionsListGL2 glExtensionsListGL2.c -lX11 -lGL
gcc -o glExtensionsListGL3 glExtensionsListGL3.c -lX11 -lGL
gcc -o contextRetargetDrawable01 contextRetargetDrawable01.c -lX11 -lGL
gcc -o contextRetargetDrawable02 contextRetargetDrawable02.c -lX11 -lGL
