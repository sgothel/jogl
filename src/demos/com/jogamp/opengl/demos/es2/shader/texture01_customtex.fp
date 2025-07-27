// Copyright 2012-2025 JogAmp Community. All rights reserved.

#if __VERSION__ >= 130
  #define varying in
  out vec4 mgl_FragColor;
  #define texture2D texture
#else
  #define mgl_FragColor gl_FragColor
#endif

varying  vec2          mgl_texCoord;
varying  vec4          frontColor;

uniform sampler2D      mgl_Texture0;
uniform usampler2D     mgl_Texture1;
uniform int            mgl_TexType;

void main (void)
{
  vec4 texColor;
  if(0.0 <= mgl_texCoord.t && mgl_texCoord.t<=1.0) {
    if( 1 == mgl_TexType ) {
        float zBits = texture2D(mgl_Texture0, mgl_texCoord).r;
        if( zBits == 1.0 ) {
            texColor = vec4(0.0, 1.0, 0.0, 1.0);
        }
        else if( mgl_texCoord.s < 0.5 ) {
            float c = zBits;
            texColor = vec4(c, c, c, 1.0);
        } else {
            float n = 1.0; // 5.0;
            float f = 100.0; // 10000.0;
            float c = (2.0 * n) / (f + n - zBits * (f - n));
            texColor = vec4(c, c, c, 1.0);
        }
    } else if( 2 == mgl_TexType ) {
        uint stencil = texture2D(mgl_Texture1, mgl_texCoord).r;
        if( stencil == 0 ) {
            texColor = vec4(0.0, 0.0, 1.0, 1.0);
        } else {
            float c = stencil; // /255.0;
            texColor = vec4(c, c, c, 1.0);
        }
    } else {
        texColor = texture2D(mgl_Texture0, mgl_texCoord);
    }
  } else {
    discard;
  }

  // mix frontColor with texture ..  pre-multiplying texture alpha
  mgl_FragColor = vec4( mix( frontColor.rgb, texColor.rgb, texColor.a ), frontColor.a );
}

