#define GLAPI

#include <GL/gl-platform.h>

// Define GL_GLEXT_PROTOTYPES so that the OpenGL GLX extension prototypes in
// "glx.h" are parsed.
// #define GL_GLEXT_PROTOTYPES
// #include <GL/gl.h>
// #include <GL/glext-supplement.h>
// #include <GL/glext.h>
#include <gl-types.h>


// Bring in the wgl extensions
#define WGL_WGLEXT_PROTOTYPES
#include <windows.h>
#include <wingdi_types.h>
#include <GL/wglext.h>
