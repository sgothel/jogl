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
    vec3 c;
    float alpha;

    if( 0 < gcu_TextureSize.z ) {
        // Pass-2: AA on Texture
        // Note: gcv_TexCoord is in center of sample pixels.

        float sampleCount = gcu_TextureSize.z;
        vec2 psize = 1.0 / gcu_TextureSize.xy; // pixel size

        float sample_weight = 1 / ( 2 * sampleCount );
        // float sample_weight = 1 / ( 2 * sampleCount + 1 );

        vec4 t = vec4(0);
        // vec4 t = texture2D(gcu_TextureUnit, gcv_TexCoord)* sample_weight; // center: +1

        // SampleCount 2
        t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2(-0.5, -0.5)))*sample_weight; // NW
        t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2(-0.5,  0.5)))*sample_weight; // SW
        t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2( 0.5,  0.5)))*sample_weight; // SE
        t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2( 0.5, -0.5)))*sample_weight; // NE
        if( sampleCount > 2 ) {
            // SampleCount 4
            t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2(-1.5, -1.5)))*sample_weight;
            t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2(-1.5,  1.5)))*sample_weight;
            t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2( 1.5,  1.5)))*sample_weight;
            t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2( 1.5, -1.5)))*sample_weight;
            if( sampleCount > 4 ) {
                // SampleCount 8
                t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2(-2.5, -2.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2(-2.5,  2.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2( 2.5,  2.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2( 2.5, -2.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2(-3.5, -3.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2(-3.5,  3.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2( 3.5,  3.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, gcv_TexCoord + psize*(vec2( 3.5, -3.5)))*sample_weight;
            }
        }
        #if 0
        if(t.w == 0.0){
            discard; // discard freezes NV tegra2 compiler
        }
        #endif
        
        c = t.rgb;
        alpha = gcu_Alpha * t.a;
    } else {
        // pass-1
        vec2 rtex = vec2(abs(gcv_TexCoord.x),abs(gcv_TexCoord.y));
        
        if( gcv_TexCoord.x == 0.0 && gcv_TexCoord.y == 0.0 ) {
             // pass-1: Lines
             c = gcu_ColorStatic.rgb;
             alpha = 1.0;
        } else if ( gcv_TexCoord.x > 0.0 && (rtex.y > 0.0 || rtex.x == 1.0) ) {
            // pass-1: curves
            rtex.y -= 0.1;
              
            if(rtex.y < 0.0 && gcv_TexCoord.y < 0.0) {
                // discard; // freezes NV tegra2 compiler
                c = zero3;
                alpha = 0.0;
            } else {
                rtex.y = max(rtex.y, 0.0);

                vec2 dtx = dFdx(rtex);
                vec2 dty = dFdy(rtex);
                  
                vec2 f = vec2((dtx.y - dtx.x + 2.0*rtex.x*dtx.x), (dty.y - dty.x + 2.0*rtex.x*dty.x));
                float position = rtex.y - (rtex.x * (1.0 - rtex.x));

                c = gcu_ColorStatic.rgb;
                alpha = clamp(0.5 - ( position/length(f) ) * sign(gcv_TexCoord.y), 0.0, 1.0);
            }
        } else {
            c = zero3;
            alpha = 0.0;
        }
    }
    mgl_FragColor = vec4(c, alpha);
}
