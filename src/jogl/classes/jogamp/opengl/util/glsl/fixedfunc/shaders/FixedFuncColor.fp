#include es_precision.glsl

#include mgl_uniform.glsl
#include mgl_varying.glsl

#include mgl_alphatest.fp

void main (void)
{
  HIGHP vec4 color = frontColor;

  if( mgl_CullFace > 0 &&
      ( ( MGL_FRONT          == mgl_CullFace &&  gl_FrontFacing ) ||
        ( MGL_BACK           == mgl_CullFace && !gl_FrontFacing ) ||
        ( MGL_FRONT_AND_BACK == mgl_CullFace ) ) ) {
      DISCARD(color);
  }
  if( mgl_AlphaTestFunc > 0 ) {
      alphaTest(color);
  }
  gl_FragColor = color;
}

