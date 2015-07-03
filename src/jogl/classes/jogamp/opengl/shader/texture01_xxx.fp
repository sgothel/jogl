// Copyright 2012 JogAmp Community. All rights reserved.

#if __VERSION__ >= 130
  #define varying in
  out vec4 mgl_FragColor;
  #define texture2D texture
#else
  #define mgl_FragColor gl_FragColor   
#endif

varying  vec2          mgl_texCoord;

uniform sampler2D      mgl_Texture0;

void main (void)
{
  mgl_FragColor = texture2D(mgl_Texture0, mgl_texCoord);
}

