
#ifndef uniforms_glsl
#define uniforms_glsl

uniform mat4    gcu_PMVMatrix[3]; // P, Mv, and Mvi
uniform vec3    gcu_ColorStatic;
uniform float   gcu_Alpha;
uniform float   gcu_Weight;
uniform sampler2D  gcu_TextureUnit;

/** 3rd component: 0: pass-1, >0: pass-2, sampleCount */
uniform vec3   gcu_TextureSize;

// const   int     MAX_TEXTURE_UNITS = 8; // <= gl_MaxTextureImageUnits 
// const   int     MAX_LIGHTS = 8; 
// uniform mat3   gcu_NormalMatrix; // transpose(inverse(ModelView)).3x3
// uniform int     gcu_ColorEnabled;
// uniform int     gcu_TexCoordEnabled[MAX_TEXTURE_UNITS];
// uniform int     gcu_CullFace;

#endif // uniforms_glsl
