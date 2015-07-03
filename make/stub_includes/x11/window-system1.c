// Routines needed from Xlib.h and Xutil.h (placed here to avoid having
// XVisualInfo generated multiple times)
#ifndef _Xconst
#define _Xconst const
#endif /* _Xconst */

#include <GL/gl-platform.h>

#include <X11/Xlib.h>
#include <X11/Xutil.h>

// Define GL_GLEXT_PROTOTYPES so that the OpenGL GLX extension prototypes in
// "glx.h" are parsed.
// #define GL_GLEXT_PROTOTYPES
// #include <GL/gl.h>
// #include <GL/glext-supplement.h>
// #include <GL/glext.h>
#include <gl-types.h>

// Define GLX_GLXEXT_PROTOTYPES so that the OpenGL GLX extension prototypes in
// "glxext.h" are parsed.
#define GLX_GLXEXT_PROTOTYPES
#include <GL/glx.h>
#include <GL/glxext.h>
