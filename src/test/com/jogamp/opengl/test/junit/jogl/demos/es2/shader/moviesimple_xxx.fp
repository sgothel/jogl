//Copyright 2010 JogAmp Community. All rights reserved.

varying  vec2          mgl_texCoord;
varying  vec4          frontColor;

void main (void)
{
  vec4 texColor = texture2D(mgl_ActiveTexture, mgl_texCoord);

  // mix frontColor with texture ..
  gl_FragColor = vec4(frontColor*texColor);
}

