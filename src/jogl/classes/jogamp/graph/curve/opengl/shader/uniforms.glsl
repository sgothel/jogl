
#ifndef uniforms_glsl
#define uniforms_glsl

uniform mat4    gcu_PMVMatrix01[3]; // P, Mv, and Mvi
uniform vec4    gcu_ColorStatic;
uniform float   gcu_Weight;

#ifdef USE_COLOR_TEXTURE
    uniform vec4  gcu_ColorTexBBox;
#endif

uniform mat4    gcu_PMVMatrix02[3]; // P, Mv, and Mvi
uniform sampler2D  gcu_FboTexUnit;

/** 
 * .x .y : texture-, fbo- or screen-size
 */
uniform vec2   gcu_FboTexSize;

#endif // uniforms_glsl
