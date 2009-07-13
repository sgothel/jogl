#define GLAPI

// Define GL_GLEXT_PROTOTYPES so that the OpenGL extension prototypes in
// "glext.h" are parsed.
#define GL_GLEXT_PROTOTYPES

#include <GL/gl.h>

// removed due to renaming and the fact that the renamed version is not included 
// in the super interfaces ..
GLAPI void APIENTRY glWeightPointer (GLint, GLenum, GLsizei, const GLvoid *);
GLAPI void APIENTRY glMatrixIndexPointer (GLint, GLenum, GLsizei, const GLvoid *);
GLAPI void APIENTRY glCurrentPaletteMatrix (GLint);
