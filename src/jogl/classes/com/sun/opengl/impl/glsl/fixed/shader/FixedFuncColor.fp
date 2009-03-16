#include es_precision.glsl

#include mgl_uniform.glsl
#include mgl_varying.glsl

void main (void)
{
  if( mgl_CullFace > 0 && 
      ( ( mgl_CullFace == 1 && gl_FrontFacing ) ||
        ( mgl_CullFace == 2 && !gl_FrontFacing ) ||
        ( mgl_CullFace == 3 ) ) ) {
    discard;
  }
  gl_FragColor = frontColor;
}

