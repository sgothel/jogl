//Copyright 2010 JogAmp Community. All rights reserved.

#include uniforms.glsl
#include varyings.glsl
#include consts.glsl

const vec3 b_color = vec3(1.0, 1.0, 1.0);
const vec4 tex_weights = vec4(0.075, 0.06, 0.045, 0.025);

void main (void)
{
    vec2 rtex = vec2(abs(gcv_TexCoord.x),abs(gcv_TexCoord.y));
    vec3 c = gcu_ColorStatic.rgb;

    float alpha = 0.0;
    
    vec2 dfx = dFdx(gcv_TexCoord);
    vec2 dfy = dFdy(gcv_TexCoord);
    
    vec2 size = 1.0/gcu_TextureSize;

    rtex -= 5.0;
    vec4 t = texture2D(gcu_TextureUnit, rtex)* 0.18;

    t += texture2D(gcu_TextureUnit, rtex + size*(vec2(1, 0)))*tex_weights.x;
    t += texture2D(gcu_TextureUnit, rtex - size*(vec2(1, 0)))*tex_weights.x;
    t += texture2D(gcu_TextureUnit, rtex + size*(vec2(0, 1)))*tex_weights.x;
    t += texture2D(gcu_TextureUnit, rtex - size*(vec2(0, 1)))*tex_weights.x;
    
    t += texture2D(gcu_TextureUnit, rtex + 2.0*size*(vec2(1, 0)))*tex_weights.y;
    t += texture2D(gcu_TextureUnit, rtex - 2.0*size*(vec2(1, 0)))*tex_weights.y;
    t += texture2D(gcu_TextureUnit, rtex + 2.0*size*(vec2(0, 1)))*tex_weights.y; 
    t += texture2D(gcu_TextureUnit, rtex - 2.0*size*(vec2(0, 1)))*tex_weights.y;
    
    t += texture2D(gcu_TextureUnit, rtex + 3.0*size*(vec2(1, 0)))*tex_weights.z;
    t += texture2D(gcu_TextureUnit, rtex - 3.0*size*(vec2(1, 0)))*tex_weights.z;
    t += texture2D(gcu_TextureUnit, rtex + 3.0*size*(vec2(0, 1)))*tex_weights.z;
    t += texture2D(gcu_TextureUnit, rtex - 3.0*size*(vec2(0, 1)))*tex_weights.z;
    
    t += texture2D(gcu_TextureUnit, rtex + 4.0*size*(vec2(1, 0)))*tex_weights.w;
    t += texture2D(gcu_TextureUnit, rtex - 4.0*size*(vec2(1, 0)))*tex_weights.w;
    t += texture2D(gcu_TextureUnit, rtex + 4.0*size*(vec2(0, 1)))*tex_weights.w;
    t += texture2D(gcu_TextureUnit, rtex - 4.0*size*(vec2(0, 1)))*tex_weights.w;
    
    if(t.w == 0.0){
        discard;
    }
    
    c = t.xyz;
    alpha = gcu_Alpha * t.w;
    gl_FragColor = vec4(c, alpha);
}
