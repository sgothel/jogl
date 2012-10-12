
#ifndef mgl_uniform_light_glsl
#define mgl_uniform_light_glsl

#include es_precision.glsl

#include mgl_const.glsl
#include mgl_lightdef.glsl

uniform LOWP    int     mgl_LightsEnabled[MAX_LIGHTS];

uniform mgl_LightModelParameters  mgl_LightModel;
uniform mgl_LightSourceParameters mgl_LightSource[MAX_LIGHTS];
uniform mgl_MaterialParameters    mgl_FrontMaterial;

#endif // mgl_uniform_light_glsl
