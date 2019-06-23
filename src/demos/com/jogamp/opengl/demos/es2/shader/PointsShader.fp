
#if __VERSION__ >= 130
  #define varying in
  out vec4 mgl_FragColor;
#else
  #define mgl_FragColor gl_FragColor   
#endif

#ifdef GL_ES
  #define MEDIUMP mediump
#else
  #define MEDIUMP
#endif

// [0].rgba: 0, smooth, attnMinSz, attnMaxSz
// [1].rgba: attnCoeff(3), attnFadeTs
uniform MEDIUMP vec4 mgl_PointParams[2]; 

#define pointSmooth                 (mgl_PointParams[0].g)

varying vec4 frontColor;

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

