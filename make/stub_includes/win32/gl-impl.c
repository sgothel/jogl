#include <windows.h>
#define GLAPI

// Define GL_GLEXT_PROTOTYPES so that the OpenGL extension prototypes in
// "glext.h" are parsed.
#define GL_GLEXT_PROTOTYPES

#include <GL/gl.h>

// Define WGL_GLEXT_PROTOTYPES so that the OpenGL WGL extension prototypes in
// "wglext.h" are parsed.
#define WGL_WGLEXT_PROTOTYPES
#define SKIP_WGL_HANDLE_DEFINITIONS

#include <GL/wglext.h>

// Generate unimplemented stubs for glX extensions
#define GLX_GLXEXT_PROTOTYPES
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <GL/glxext.h>

