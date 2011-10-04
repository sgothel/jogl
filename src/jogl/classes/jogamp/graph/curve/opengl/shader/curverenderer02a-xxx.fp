//Copyright 2010 JogAmp Community. All rights reserved.
 
//
// 1-pass shader w/ weight
//

#include uniforms.glsl
#include varyings.glsl

void main (void)
{
    vec2 rtex = vec2(abs(gcv_TexCoord.x),abs(gcv_TexCoord.y));
    vec3 c = gcu_ColorStatic.rgb;
    
    float alpha = 0.0;
    
    if((gcv_TexCoord.x == 0.0) && (gcv_TexCoord.y == 0.0)) {
         alpha = gcu_Alpha;
    }
    else if ((gcv_TexCoord.x > 0.0) && (rtex.y > 0.0 || rtex.x == 1.0)) {
        rtex.y -= 0.1;
        
        if(rtex.y < 0.0 && gcv_TexCoord.y < 0.0) {
            // discard; // freezes NV tegra2 compiler
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

            // FIXME: will we ever set gcu_Alpha != 1.0 ? If not, a==alpha!
            float a = clamp(0.5 - ( position/length(f) ) * sign(gcv_TexCoord.y), 0.0, 1.0);
            alpha = gcu_Alpha * a;
        }
    }

    gl_FragColor = vec4(c, alpha);
}
