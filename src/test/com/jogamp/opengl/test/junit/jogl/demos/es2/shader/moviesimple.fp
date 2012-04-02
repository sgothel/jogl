
#ifdef GL_ES
  #define MEDIUMP mediump
  #define HIGHP highp
#else
  #define MEDIUMP
  #define HIGHP
#endif

uniform   sampler2D     mgl_ActiveTexture;
varying   HIGHP vec4    mgl_texCoord;
varying   HIGHP vec4    frontColor;

void main (void)
{
  vec4 texColor = texture2D(mgl_ActiveTexture, mgl_texCoord.st);

  // mix frontColor with texture ..
  gl_FragColor = vec4(frontColor.rgb*texColor.rgb, frontColor.a);
}

