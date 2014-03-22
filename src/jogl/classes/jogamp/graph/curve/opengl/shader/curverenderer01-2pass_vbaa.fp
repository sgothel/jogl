//Copyright 2010 JogAmp Community. All rights reserved.

//
// 2-pass shader w/o weight
//

#if __VERSION__ >= 130
  #define varying in
  out vec4 mgl_FragColor;
  #define texture2D texture
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

    // Note: gcu_Alpha is multiplied in pass2!
    
    if( 0 < gcu_TextureSize.z ) {

// 1st Choice VBAA
#include curverenderer01-pass2-vbaa_poles_equalweight.glsl

// #include curverenderer01-pass2-vbaa_poles_bilin1.glsl
// #include curverenderer01-pass2-vbaa_poles_propweight1.glsl
// #include curverenderer01-pass2-vbaa_wholeedge_propweight1.glsl
// #include curverenderer01-pass2-vbaa_wholeedge_equalweight.glsl
// #include curverenderer01-pass2-vbaa_fxaa3.glsl

    } else {

#include curverenderer01-pass1-curve-simple.glsl

    }
    mgl_FragColor = vec4(color, alpha);
}
