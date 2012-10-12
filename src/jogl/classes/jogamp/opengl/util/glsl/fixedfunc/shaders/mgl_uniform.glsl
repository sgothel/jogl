
#ifndef mgl_uniform_glsl
#define mgl_uniform_glsl

#include es_precision.glsl

#include mgl_const.glsl

uniform HIGHP   mat4    mgl_PMVMatrix[4]; // P, Mv, Mvi and Mvit (transpose(inverse(ModelView)) == normalMatrix)
uniform LOWP    int     mgl_ColorEnabled;
uniform HIGHP   vec4    mgl_ColorStatic;
uniform LOWP    int     mgl_AlphaTestFunc;
uniform HIGHP   float   mgl_AlphaTestRef;
uniform LOWP    int     mgl_TextureEnabled[MAX_TEXTURE_UNITS];
uniform LOWP    int     mgl_TexCoordEnabled[MAX_TEXTURE_UNITS];
uniform LOWP    int     mgl_TexEnvMode[MAX_TEXTURE_UNITS];
uniform LOWP    int     mgl_TexFormat[MAX_TEXTURE_UNITS];
uniform   sampler2D     mgl_Texture0;
uniform   sampler2D     mgl_Texture1;
uniform   sampler2D     mgl_Texture2;
uniform   sampler2D     mgl_Texture3;
uniform   sampler2D     mgl_Texture4;
uniform   sampler2D     mgl_Texture5;
uniform   sampler2D     mgl_Texture6;
uniform   sampler2D     mgl_Texture7;
uniform   sampler2D     mgl_Texture8;
uniform LOWP    int     mgl_CullFace;

#endif // mgl_uniform_glsl
