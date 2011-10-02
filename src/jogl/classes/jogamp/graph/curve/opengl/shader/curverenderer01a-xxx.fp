//Copyright 2010 JogAmp Community. All rights reserved.

#include uniforms.glsl
#include varyings.glsl

const vec3 b_color = vec3(1.0, 1.0, 1.0);
const vec4 tex_weights = vec4(0.075, 0.06, 0.045, 0.025);

void main (void)
{
    vec2 rtex = vec2(abs(gcv_TexCoord.x),abs(gcv_TexCoord.y));
    vec3 c = gcu_ColorStatic.rgb;
    
    float alpha = 0.0;
    
    if((gcv_TexCoord.x == 0.0) && (gcv_TexCoord.y == 0.0)) {
         alpha = gcu_Alpha;
    }
    else if ((gcv_TexCoord.x > 0.0) && (rtex.y > 0.0 || rtex.x == 1.0)) {
        vec2 dtx = dFdx(rtex);
        vec2 dty = dFdy(rtex);
          
        rtex.y -= 0.1;
          
        if(rtex.y < 0.0) {
            /** discard freezes NV tegra2 compiler (TODO: dbl check)
            if(gcv_TexCoord.y < 0.0) {
                discard;
            } else {
                rtex.y = 0.0;
            }*/
            rtex.y = 0.0;
        }
          
        vec2 f = vec2((dtx.y - dtx.x + 2.0*rtex.x*dtx.x), (dty.y - dty.x + 2.0*rtex.x*dty.x));
        float position = rtex.y - (rtex.x * (1.0 - rtex.x));
        float d = position/(length(f));

        float a = (0.5 - d * sign(gcv_TexCoord.y));  
        
        if (a >= 1.0)  { 
            alpha = gcu_Alpha;
        } else if (a <= 0.0) {
            // discard; // freezes NV tegra2 compiler (TODO: dbl check)
            alpha = 0.0;
        } else {           
            alpha = gcu_Alpha * a;
            // ?? mix(b_color,gcu_ColorStatic.rgb, a);
        }
    }
    
    gl_FragColor = vec4(c, alpha);
}
