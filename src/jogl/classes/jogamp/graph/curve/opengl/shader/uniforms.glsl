
#ifndef uniforms_glsl
#define uniforms_glsl

#include precision.glsl

// #include consts.glsl

uniform HIGHP mat4    gcu_PMVMatrix[3]; // P, Mv, and Mvi
uniform HIGHP vec3    gcu_ColorStatic;
uniform HIGHP float   gcu_Alpha;
uniform HIGHP float   gcu_Weight;
uniform sampler2D     gcu_TextureUnit;

// #if __VERSION__ < 130
 uniform HIGHP vec2   gcu_TextureSize;
// #endif

// uniform HIGHP mat3    gcu_NormalMatrix; // transpose(inverse(ModelView)).3x3
// uniform LOWP  int     gcu_ColorEnabled;
// uniform LOWP  int     gcu_TexCoordEnabled[MAX_TEXTURE_UNITS];
// uniform LOWP  int     gcu_CullFace;

#endif // uniforms_glsl
