
#include es_precision.glsl
#include mgl_lightdef.glsl

#include mgl_const.glsl
#include mgl_uniform.glsl
#include mgl_varying.glsl

vec4 getTexColor(in sampler2D tex, in int idx) {
    vec4 coord;
    if(idx==0) {
        coord= mgl_TexCoords[0];
    } else if(idx==1) {
        coord= mgl_TexCoords[1];
    } else if(idx==2) {
        coord= mgl_TexCoords[2];
    } else if(idx==3) {
        coord= mgl_TexCoords[3];
    } else if(idx==4) {
        coord= mgl_TexCoords[4];
    } else if(idx==5) {
        coord= mgl_TexCoords[5];
    } else if(idx==6) {
        coord= mgl_TexCoords[6];
    } else {
        coord= mgl_TexCoords[7];
    }
    return texture2D(tex, coord.st);
}

void main (void)
{
  if( mgl_CullFace > 0 && 
      ( ( mgl_CullFace == 1 && gl_FrontFacing ) ||
        ( mgl_CullFace == 2 && !gl_FrontFacing ) ||
        ( mgl_CullFace == 3 ) ) ) {
    discard;
  }

  vec4 texColor = getTexColor(mgl_ActiveTexture,mgl_ActiveTextureIdx);

  if(length(texColor.rgb)>0.0) {
    gl_FragColor = vec4(frontColor.rgb*texColor.rgb, frontColor.a) ;
  } else {
    gl_FragColor = frontColor;
  }
}
