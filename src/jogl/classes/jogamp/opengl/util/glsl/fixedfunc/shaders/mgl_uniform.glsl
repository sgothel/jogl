
#ifndef mgl_uniform_glsl
#define mgl_uniform_glsl

#include es_precision.glsl

#include mgl_const.glsl

uniform HIGHP   mat4    mgl_PMVMatrix[4]; // P, Mv, Mvi and Mvit (transpose(inverse(ModelView)) == normalMatrix)
uniform LOWP    int     mgl_ColorEnabled;
uniform HIGHP   vec4    mgl_ColorStatic;
uniform LOWP    int     mgl_TexCoordEnabled[MAX_TEXTURE_UNITS];
uniform       sampler2D mgl_ActiveTexture;
uniform LOWP    int     mgl_ActiveTextureIdx;
uniform LOWP    int     mgl_CullFace;

#endif // mgl_uniform_glsl
