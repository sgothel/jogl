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

uniform sampler2D      mgl_ActiveTexture;

void main (void)
{
  vec4 texColor;
  if(0.0 <= mgl_texCoord.t && mgl_texCoord.t<=1.0) {
    texColor = texture2D(mgl_ActiveTexture, mgl_texCoord);
  } else {
    discard;
  }

  // mix frontColor with texture ..  pre-multiplying texture alpha
  mgl_FragColor = vec4( mix( frontColor.rgb, texColor.rgb, texColor.a ), frontColor.a );
}

