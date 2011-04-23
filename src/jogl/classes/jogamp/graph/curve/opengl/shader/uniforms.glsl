
#ifndef uniforms_glsl
#define uniforms_glsl

#include precision.glsl

// #include consts.glsl

uniform HIGHP mat4    gcu_PMVMatrix[3]; // P, Mv, and Mvi
uniform HIGHP vec3    gcu_ColorStatic;
uniform HIGHP float   gcu_Alpha;
uniform HIGHP float   gcu_P1Y;
uniform HIGHP float   gcu_Strength;
uniform sampler2D     gcu_TextureUnit;

// uniform HIGHP mat3    gcu_NormalMatrix; // transpose(inverse(ModelView)).3x3
// uniform LOWP  int     gcu_ColorEnabled;
// uniform LOWP  int     gcu_TexCoordEnabled[MAX_TEXTURE_UNITS];
// uniform LOWP  int     gcu_CullFace;

#endif // uniforms_glsl
