//Copyright 2010 JogAmp Community. All rights reserved.

//
// 1-pass shader w/o weight
//

#if __VERSION__ >= 130
  #define varying in
  out vec4 mgl_FragColor;
#else
  #define mgl_FragColor gl_FragColor
#endif

#include uniforms.glsl
#include varyings.glsl

void main (void)
{

// #include curverenderer01-pass1-curve-lineAA.glsl
#include curverenderer01-pass1-curve-weight.glsl

}
