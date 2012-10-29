
#if __VERSION__ >= 130
  #define varying in
  out vec4 mgl_FragColor;
#else
  #define mgl_FragColor gl_FragColor   
#endif


#include es_precision.glsl
#include mgl_lightdef.glsl

#include mgl_const.glsl
#include mgl_uniform.glsl
#include mgl_varying.glsl

// #define TEST 1

void main (void)
{ 
  mgl_FragColor = frontColor;

  if( pointSmooth > 0.5 ) {
      // smooth (AA)
      const float border = 0.90; // take/give 10% for AA

      // origin to 0/0, [-1/-1 .. 1/1]
      vec2 pointPos = 2.0 * gl_PointCoord - 1.0 ;
      float r = length( pointPos ); // one-circle sqrt(x * x + y * y), range: in-circle [0..1], out >1
      float r1 = 1.0 - ( step(border, r) * 10.0 * ( r - border ) ) ; // [0..1]
      #ifndef TEST
          if( r1 < 0.0 ) {
            discard;
          }
      #endif

      #ifndef TEST
          mgl_FragColor.a *= r1;
      #else
          mgl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
          mgl_FragColor.r = r1 < 0.0 ? 1.0 : 0.0;
          mgl_FragColor.g = r > 1.0 ? 1.0 : 0.0;
          mgl_FragColor.b = r > border ? 1.0 : 0.0;
      #endif
  }
}

