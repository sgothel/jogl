
#ifndef mgl_attribute_glsl
#define mgl_attribute_glsl

#include es_precision.glsl

attribute vec4    mgl_Vertex;
attribute vec4    mgl_Normal;
attribute vec4    mgl_Color;
#if MAX_TEXTURE_UNITS >= 2
attribute vec4    mgl_MultiTexCoord0;
attribute vec4    mgl_MultiTexCoord1;
#endif
#if MAX_TEXTURE_UNITS >= 4
attribute vec4    mgl_MultiTexCoord2;
attribute vec4    mgl_MultiTexCoord3;
#endif
#if MAX_TEXTURE_UNITS >= 8
attribute vec4    mgl_MultiTexCoord4;
attribute vec4    mgl_MultiTexCoord5;
attribute vec4    mgl_MultiTexCoord6;
attribute vec4    mgl_MultiTexCoord7;
#endif

#endif // mgl_attribute_glsl
