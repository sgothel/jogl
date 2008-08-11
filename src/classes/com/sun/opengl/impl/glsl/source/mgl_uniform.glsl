
#ifndef mgl_uniform_glsl
#define mgl_uniform_glsl

#include es_precision.glsl

#include mgl_const.glsl
#include mgl_lightdef.glsl

uniform   HIGHP mat4    mgl_PMVMatrix[3]; // P, Mv, and Mvi
uniform   HIGHP mat3    mgl_NormalMatrix; // transpose(inverse(ModelView)).3x3
uniform MEDIUMP int     mgl_ColorEnabled;
uniform   HIGHP vec4    mgl_ColorStatic;
uniform MEDIUMP int     mgl_TexCoordEnabled;
uniform MEDIUMP int     mgl_LightsEnabled;
uniform mgl_LightSourceParameters mgl_LightSource[MAX_LIGHTS];
uniform mgl_MaterialParameters    mgl_FrontMaterial;
uniform   HIGHP sampler2D mgl_ActiveTexture;
uniform MEDIUMP int       mgl_ActiveTextureIdx;

#endif
