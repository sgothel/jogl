
#ifndef uniforms_glsl
#define uniforms_glsl

uniform mat4    gcu_PMVMatrix[3]; // P, Mv, and Mvi
uniform vec3    gcu_ColorStatic;
uniform float   gcu_Alpha;
uniform float   gcu_Weight;
uniform sampler2D      gcu_TextureUnit;

// #if __VERSION__ < 130
uniform vec2    gcu_TextureSize;
// #endif

// const   int     MAX_TEXTURE_UNITS = 8; // <= gl_MaxTextureImageUnits 
// const   int     MAX_LIGHTS = 8; 
// uniform mat3   gcu_NormalMatrix; // transpose(inverse(ModelView)).3x3
// uniform int     gcu_ColorEnabled;
// uniform int     gcu_TexCoordEnabled[MAX_TEXTURE_UNITS];
// uniform int     gcu_CullFace;

#endif // uniforms_glsl
