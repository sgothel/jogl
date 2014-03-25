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

const vec3 zero3 = vec3(0);

void main (void)
{
    vec3 color;
    float alpha;
    
    if( 0 < gcu_TextureSize.z ) {
        // Pass-2: Dump Texture
        vec4 t = texture2D(gcu_TextureUnit, gcv_TexCoord.st);
        #if 0
        if( 0.0 == t.a ) {
          discard; // discard freezes NV tegra2 compiler
        }
        #endif

        color = t.rgb;   
        alpha = gcu_Alpha * t.a;
    } else {

#include curverenderer01-pass1-curve-weight.glsl

    }
    mgl_FragColor = vec4(color, alpha);
}
