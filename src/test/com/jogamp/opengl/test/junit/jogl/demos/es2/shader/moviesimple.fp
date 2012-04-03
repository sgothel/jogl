
#ifdef GL_ES
  #define MEDIUMP mediump
  #define HIGHP highp
#else
  #define MEDIUMP
  #define HIGHP
#endif

uniform  sampler2D     mgl_ActiveTexture;
varying  MEDIUMP vec2  mgl_texCoord;
varying  MEDIUMP vec4  frontColor;

void main (void)
{
  HIGHP vec4 texColor = texture2D(mgl_ActiveTexture, mgl_texCoord);

  // mix frontColor with texture ..
  gl_FragColor = vec4(frontColor*texColor);
}

