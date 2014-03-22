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

const vec3 zero3 = vec3(0);

void main (void)
{
    vec3 color;
    float alpha;

// #include curverenderer01-pass1-curve-lineAA.glsl
#include curverenderer01-pass1-curve-simple.glsl

    mgl_FragColor = vec4(color, gcu_Alpha * alpha);
}
