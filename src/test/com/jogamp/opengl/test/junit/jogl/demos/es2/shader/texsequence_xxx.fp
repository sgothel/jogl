// Copyright 2012 JogAmp Community. All rights reserved.

#if __VERSION__ >= 130
  #define varying in
  out vec4 mgl_FragColor;
  #define texture2D texture
#else
  #define mgl_FragColor gl_FragColor   
#endif

varying  vec2          mgl_texCoord;
varying  vec4          frontColor;

// Insert dynamic code after the following tag:
// TEXTURE-SEQUENCE-CODE-BEGIN
// TEXTURE-SEQUENCE-CODE-END

void main (void)
{
  vec4 texColor;
  if(0.0 <= mgl_texCoord.t && mgl_texCoord.t<=1.0) {
    texColor = myTexture2D(mgl_ActiveTexture, mgl_texCoord);
  } else {
    texColor = vec4(1, 1, 1, 1);
  }

  // mix frontColor with texture ..
  mgl_FragColor = vec4(frontColor*texColor);
}

