#define GLAPI

// Define GL_GLEXT_PROTOTYPES so that the OpenGL extension prototypes in
// "glext.h" are parsed.
#define GL_GLEXT_PROTOTYPES

#include <GL/gl.h>

// Bring in the wgl extensions
#define WGL_WGLEXT_PROTOTYPES
#define SKIP_WGL_HANDLE_DEFINITIONS
#include <windows.h>
#include <GL/wglext.h>
