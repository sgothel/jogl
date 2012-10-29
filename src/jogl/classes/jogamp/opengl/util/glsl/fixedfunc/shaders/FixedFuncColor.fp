
#if __VERSION__ >= 130
  #define varying in
  out vec4 mgl_FragColor;
#else
  #define mgl_FragColor gl_FragColor   
#endif

#include es_precision.glsl

#include mgl_uniform.glsl
#include mgl_varying.glsl

#include mgl_alphatest.fp

void main (void)
{
  vec4 color = frontColor;

  /** ES2 supports CullFace implicit ..
  if( mgl_CullFace > 0 &&
      ( ( MGL_FRONT          == mgl_CullFace &&  gl_FrontFacing ) ||
        ( MGL_BACK           == mgl_CullFace && !gl_FrontFacing ) ||
        ( MGL_FRONT_AND_BACK == mgl_CullFace ) ) ) {
      DISCARD(color);
  } */
  if( mgl_AlphaTestFunc > 0 ) {
      alphaTest(color);
  }
  mgl_FragColor = color;
}

