
#include es_precision.glsl
#include mgl_lightdef.glsl

#include mgl_const.glsl
#include mgl_uniform.glsl
#include mgl_varying.glsl

void main (void)
{
    vec4 texColor = texture2D(mgl_ActiveTexture,mgl_TexCoord[mgl_ActiveTextureIdx].st);
    if(greaterThan(texColor, zero)) {
       gl_FragColor = vec4(frontColor.rgb*texColor.rgb, frontColor.a * texColor.a) ; 
    } else {
       gl_FragColor = frontColor;
    }
}

