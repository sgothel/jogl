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
    vec3 c;
    float alpha;

    /**
     * CDTriangulator2D.extractBoundaryTriangles(..):
     *     0 > gcv_TexCoord.y : hole or holeLike
     *     0 < gcv_TexCoord.y : !hole (outer)
     *      
     *     0   == gcv_TexCoord.x : vertex-0 of triangle
     *     0.5 == gcv_TexCoord.x : vertex-1 of triangle
     *     1   == gcv_TexCoord.x : vertex-2 of triangle
     */
    vec2 rtex = vec2(abs(gcv_TexCoord.x),abs(gcv_TexCoord.y));
    
    if( gcv_TexCoord.x == 0.0 && gcv_TexCoord.y == 0.0 ) {
         // pass-1: Lines
         c = gcu_ColorStatic.rgb;
         alpha = gcu_Alpha;
    } else if ( gcv_TexCoord.x > 0.0 && ( rtex.y > 0.0 || rtex.x == 1.0 ) ) {
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
            alpha = gcu_Alpha * clamp(0.5 - ( position/length(f) ) * sign(gcv_TexCoord.y), 0.0, 1.0);
        }
    } else {
        c = zero3;
        alpha = 0.0;
    }
    mgl_FragColor = vec4(c, alpha);
}
