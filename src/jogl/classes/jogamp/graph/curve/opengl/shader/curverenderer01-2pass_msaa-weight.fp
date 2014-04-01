//Copyright 2010 JogAmp Community. All rights reserved.
 
//
// 2-pass shader w/ weight
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

void main (void)
{
    if( 0.0 < gcu_FboTexSize.z ) {
        // Pass-2: Dump Texture
        vec4 t = texture2D(gcu_FboTexUnit, gcv_FboTexCoord.st);
        #if 0
        if( 0.0 == t.a ) {
          discard; // discard freezes NV tegra2 compiler
        }
        #endif

        mgl_FragColor = t;
    } else {

#include curverenderer01-pass1-curve-weight.glsl

    }
}
