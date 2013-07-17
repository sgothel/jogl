#define GL_APICALL
#define GL_APIENTRY

// Define GL_GLEXT_PROTOTYPES so that the OpenGL extension prototypes in
// "glext.h" are parsed.
#define GL_GLEXT_PROTOTYPES

#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>

/** We assume ES2 extensions maybe avail on ES3 .. */
#include <GLES2/gl2ext.h>
