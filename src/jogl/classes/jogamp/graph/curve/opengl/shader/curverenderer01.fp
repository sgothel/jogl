//Copyright 2010 JogAmp Community. All rights reserved.

/**
 * AMD complains: #version must occur before any other statement in the program
#ifdef GL_ES
    #version 100
#else
    #version 110
#endif
 */
 
#include uniforms.glsl
#include varyings.glsl

const vec3 b_color = vec3(0.0, 0.0, 0.0);
const vec4 weights = vec4(0.075, 0.06, 0.045, 0.025);

void main (void)
{
    vec2 rtex = vec2(abs(gcv_TexCoord.x),abs(gcv_TexCoord.y));
    vec3 c = gcu_ColorStatic.rgb;
    
    float alpha = 0.0;
    
    if((gcv_TexCoord.x == 0.0) && (gcv_TexCoord.y == 0.0)){
         alpha = gcu_Alpha;
    }
    else if((gcv_TexCoord.x >= 5.0)){
        vec2 dfx = dFdx(gcv_TexCoord);
        vec2 dfy = dFdy(gcv_TexCoord);
        
        vec2 size = 1.0/textureSize(gcu_TextureUnit,0); //version 130 - FIXME: replace with uniform value
        rtex -= 5.0;
        vec4 t = texture2D(gcu_TextureUnit, rtex)* 0.18;

        t += texture2D(gcu_TextureUnit, rtex + size*(vec2(1, 0)))*weights.x;
        t += texture2D(gcu_TextureUnit, rtex - size*(vec2(1, 0)))*weights.x;
        t += texture2D(gcu_TextureUnit, rtex + size*(vec2(0, 1)))*weights.x;
        t += texture2D(gcu_TextureUnit, rtex - size*(vec2(0, 1)))*weights.x;
        
        t += texture2D(gcu_TextureUnit, rtex + 2.0*size*(vec2(1, 0)))*weights.y;
        t += texture2D(gcu_TextureUnit, rtex - 2.0*size*(vec2(1, 0)))*weights.y;
        t += texture2D(gcu_TextureUnit, rtex + 2.0*size*(vec2(0, 1)))*weights.y; 
        t += texture2D(gcu_TextureUnit, rtex - 2.0*size*(vec2(0, 1)))*weights.y;
        
        t += texture2D(gcu_TextureUnit, rtex + 3.0*size*(vec2(1, 0)))*weights.z;
        t += texture2D(gcu_TextureUnit, rtex - 3.0*size*(vec2(1, 0)))*weights.z;
        t += texture2D(gcu_TextureUnit, rtex + 3.0*size*(vec2(0, 1)))*weights.z;
        t += texture2D(gcu_TextureUnit, rtex - 3.0*size*(vec2(0, 1)))*weights.z;
        
        t += texture2D(gcu_TextureUnit, rtex + 4.0*size*(vec2(1, 0)))*weights.w;
        t += texture2D(gcu_TextureUnit, rtex - 4.0*size*(vec2(1, 0)))*weights.w;
        t += texture2D(gcu_TextureUnit, rtex + 4.0*size*(vec2(0, 1)))*weights.w;
        t += texture2D(gcu_TextureUnit, rtex - 4.0*size*(vec2(0, 1)))*weights.w;
        
        if(t.w == 0.0){
            discard;
        }
        
        c = t.xyz;
        alpha = gcu_Alpha * t.w;
    }
    ///////////////////////////////////////////////////////////
    else if ((gcv_TexCoord.x > 0.0) && (rtex.y > 0.0 || rtex.x == 1.0)){
        vec2 dtx = dFdx(rtex);
        vec2 dty = dFdy(rtex);
          
        rtex.y -= 0.1;
          
        if(rtex.y < 0.0) {
            if(gcv_TexCoord.y < 0.0)
                discard;
            else{
                rtex.y = 0.0;
            }
        }
          
        vec2 f = vec2((dtx.y - dtx.x + 2.0*rtex.x*dtx.x), (dty.y - dty.x + 2.0*rtex.x*dty.x));
        float position = rtex.y - (rtex.x * (1.0 - rtex.x));
        float d = position/(length(f));

        float a = (0.5 - d * sign(gcv_TexCoord.y));  
        
        if (a >= 1.0)  { 
            alpha = gcu_Alpha;
        }  
        else if (a <= 0.0) {
            alpha = 0.0;//discard;
        }
        else {           
            alpha = gcu_Alpha * a;
            mix(b_color,gcu_ColorStatic.rgb, a);
        }
    }
    
    gl_FragColor = vec4(c, alpha);
}
