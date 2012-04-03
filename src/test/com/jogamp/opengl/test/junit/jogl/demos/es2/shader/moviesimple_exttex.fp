
#extension GL_OES_EGL_image_external : require

#define MEDIUMP mediump
#define HIGHP highp

uniform  samplerExternalOES  mgl_ActiveTexture;
varying  MEDIUMP vec2        mgl_texCoord;
varying  MEDIUMP vec4        frontColor;

void main (void)
{
  HIGHP vec4 texColor = texture2D(mgl_ActiveTexture, mgl_texCoord);

  // mix frontColor with texture ..
  gl_FragColor = vec4(frontColor*texColor);
}

