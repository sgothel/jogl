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
    vec3 c;
    float alpha;
    
    if( 0 < gcu_TextureSize.z ) {
        // Pass-2: Dump Texture
        vec4 t = texture2D(gcu_TextureUnit, gcv_TexCoord);
        #if 0
        if( 0.0 == t.a ) {
          discard; // discard freezes NV tegra2 compiler
        }
        #endif

        c = t.rgb;   
        alpha = gcu_Alpha * t.a;
    } else {
        // Pass-1
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
                  
                float w = gcu_Weight;
                float pd = ((2.0 - (2.0*w))*rtex.x*rtex.x) + 2.0*(w-1.0)*rtex.x + 1.0;
                float position = rtex.y - ((w*rtex.x*(1.0 - rtex.x))/pd);

                float aph = 2.0 - 2.0*w;
                
                float gd = (aph*rtex.x*rtex.x + 2.0*rtex.x + 1.0)*(aph*rtex.x*rtex.x + 2.0*rtex.x + 1.0);
                vec2 f = vec2((dtx.y - (w*dtx.x*(1.0 - 2.0*rtex.x))/gd), (dty.y - (w*dty.x*(1.0 - 2.0*rtex.x))/gd));

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
