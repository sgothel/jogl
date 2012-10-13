
#ifndef mgl_attribute_glsl
#define mgl_attribute_glsl

#include es_precision.glsl

attribute HIGHP vec4    mgl_Vertex;
attribute HIGHP vec4    mgl_Normal;
attribute HIGHP vec4    mgl_Color;
#if MAX_TEXTURE_UNITS >= 2
attribute HIGHP vec4    mgl_MultiTexCoord0;
attribute HIGHP vec4    mgl_MultiTexCoord1;
#endif
#if MAX_TEXTURE_UNITS >= 4
attribute HIGHP vec4    mgl_MultiTexCoord2;
attribute HIGHP vec4    mgl_MultiTexCoord3;
#endif
#if MAX_TEXTURE_UNITS >= 8
attribute HIGHP vec4    mgl_MultiTexCoord4;
attribute HIGHP vec4    mgl_MultiTexCoord5;
attribute HIGHP vec4    mgl_MultiTexCoord6;
attribute HIGHP vec4    mgl_MultiTexCoord7;
#endif

#endif // mgl_attribute_glsl
