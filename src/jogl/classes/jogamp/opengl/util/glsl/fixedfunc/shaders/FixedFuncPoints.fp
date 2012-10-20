
#include es_precision.glsl
#include mgl_lightdef.glsl

#include mgl_const.glsl
#include mgl_uniform.glsl
#include mgl_varying.glsl

void main (void)
{ 
  // FIXME: Since gl_Points must be 1.0 (otherwise no points)
  //         don't see reason for fetching texture color.
  // gl_FragColor = frontColor * texture2D(mgl_Texture0, gl_PointCoord);
  gl_FragColor = frontColor;
}

