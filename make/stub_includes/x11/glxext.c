#define GLAPI

// Define GL_GLEXT_PROTOTYPES so that the OpenGL extension prototypes in
// "glext.h" are parsed.
#define GL_GLEXT_PROTOTYPES

#include <GL/gl.h>

// Define GLX_GLXEXT_PROTOTYPES so that the OpenGL GLX extension prototypes in
// "glxext.h" are parsed.
#define GLX_GLXEXT_PROTOTYPES
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <GL/glxext.h>
