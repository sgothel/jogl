//Copyright 2010 JogAmp Community. All rights reserved.

#if __VERSION__ >= 130
  #define varying in
  out vec4 mgl_FragData[2];
  #define texture2D texture
#else
  #define mgl_FragData gl_FragData   
#endif

uniform sampler2D gcs_TexUnit0;
uniform sampler2D gcs_TexUnit1;

varying vec4    frontColor;
varying vec2      texCoord;

void main (void)
{
  vec2 rg = texture2D(gcs_TexUnit0, texCoord).rg + texture2D(gcs_TexUnit1, texCoord).rg;
  float b = frontColor.b - length(rg);
  mgl_FragData[0] = vec4( rg, b, 1.0 );
}
