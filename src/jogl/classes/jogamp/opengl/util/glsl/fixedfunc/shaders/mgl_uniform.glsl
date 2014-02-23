
#ifndef mgl_uniform_glsl
#define mgl_uniform_glsl

#include es_precision.glsl

#include mgl_const.glsl

uniform         mat4    mgl_PMVMatrix[4]; // P, Mv, Mvi and Mvit (transpose(inverse(ModelView)) == normalMatrix)
uniform LOWP    int     mgl_ColorEnabled;
uniform         vec4    mgl_ColorStatic;
uniform LOWP    int     mgl_AlphaTestFunc;
uniform         float   mgl_AlphaTestRef;

// [0].rgba: size, smooth, attnMinSz, attnMaxSz
// [1].rgba: attnCoeff(3), attnFadeTs
uniform MEDIUMP vec4    mgl_PointParams[2];

#define pointSize                   (mgl_PointParams[0].r)
#define pointSmooth                 (mgl_PointParams[0].g)
#define pointSizeMin                (mgl_PointParams[0].b)
#define pointSizeMax                (mgl_PointParams[0].a)
#define pointDistanceConstantAtten  (mgl_PointParams[1].r)
#define pointDistanceLinearAtten    (mgl_PointParams[1].g)
#define pointDistanceQuadraticAtten (mgl_PointParams[1].b)
#define pointFadeThresholdSize      (mgl_PointParams[1].a)

// uniform LOWP int    mgl_CullFace; // ES2 supports CullFace implicit ..
#if MAX_TEXTURE_UNITS > 0
uniform LOWP    int     mgl_TextureEnabled[MAX_TEXTURE_UNITS];
uniform LOWP    int     mgl_TexCoordEnabled[MAX_TEXTURE_UNITS];
uniform LOWP    int     mgl_TexEnvMode[MAX_TEXTURE_UNITS];
uniform LOWP    int     mgl_TexFormat[MAX_TEXTURE_UNITS];
#if MAX_TEXTURE_UNITS >= 2
uniform   sampler2D     mgl_Texture0;
uniform   sampler2D     mgl_Texture1;
#endif
#if MAX_TEXTURE_UNITS >= 4
uniform   sampler2D     mgl_Texture2;
uniform   sampler2D     mgl_Texture3;
#endif
#if MAX_TEXTURE_UNITS >= 8
uniform   sampler2D     mgl_Texture4;
uniform   sampler2D     mgl_Texture5;
uniform   sampler2D     mgl_Texture6;
uniform   sampler2D     mgl_Texture7;
#endif
#endif

#endif // mgl_uniform_glsl
