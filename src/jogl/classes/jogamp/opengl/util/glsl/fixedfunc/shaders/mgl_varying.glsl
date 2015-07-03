
#ifndef mgl_varying_glsl
#define mgl_varying_glsl

#include es_precision.glsl

#include mgl_const.glsl

varying   vec4    frontColor;
#if MAX_TEXTURE_UNITS > 0
varying   vec4    mgl_TexCoords[MAX_TEXTURE_UNITS];
#endif

#endif // mgl_varying_glsl
