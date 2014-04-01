
#ifndef uniforms_glsl
#define uniforms_glsl

uniform mat4    gcu_PMVMatrix01[3]; // P, Mv, and Mvi
uniform vec4    gcu_ColorStatic;
uniform float   gcu_Weight;

uniform mat4    gcu_PMVMatrix02[3]; // P, Mv, and Mvi
uniform sampler2D  gcu_FboTexUnit;

/** 
 * .x .y : texture-, fbo- or screen-size
 */
uniform vec2   gcu_FboTexSize;

// const   int     MAX_TEXTURE_UNITS = 8; // <= gl_MaxTextureImageUnits 
// const   int     MAX_LIGHTS = 8; 
// uniform mat3   gcu_NormalMatrix; // transpose(inverse(ModelView)).3x3
// uniform int     gcu_ColorEnabled;
// uniform int     gcu_TexCoordEnabled[MAX_TEXTURE_UNITS];
// uniform int     gcu_CullFace;

#endif // uniforms_glsl
