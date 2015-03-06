// Routines needed from Xlib.h and Xutil.h (placed here to avoid having
// XVisualInfo generated multiple times)
#ifndef _Xconst
#define _Xconst const
#endif /* _Xconst */

// Define GL_GLEXT_PROTOTYPES so that the OpenGL GLX extension prototypes in
// "glx.h" are parsed.
#define GL_GLEXT_PROTOTYPES

// Define GLX_GLXEXT_PROTOTYPES so that the OpenGL GLX extension prototypes in
// "glxext.h" are parsed.
#define GLX_GLXEXT_PROTOTYPES
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <GL/glx.h>
// GL/glxext.h included by GL/glx.h
