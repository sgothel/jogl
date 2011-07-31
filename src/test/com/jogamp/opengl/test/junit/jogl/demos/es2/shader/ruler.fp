//Copyright 2010 JogAmp Community. All rights reserved.

#ifdef GL_ES
  #define MEDIUMP mediump
  #define HIGHP highp
  #define LOWP  lowp
#else
  #define MEDIUMP
  #define HIGHP
  #define LOWP
#endif

uniform MEDIUMP vec3 gcu_RulerColor;
uniform MEDIUMP vec2 gcu_RulerPixFreq;

const MEDIUMP vec2 onev2 = vec2(1.0, 1.0);

void main (void)
{
  MEDIUMP vec2 c = step( onev2, mod(gl_FragCoord.xy, gcu_RulerPixFreq) );
  if( c.s == 0.0 || c.t == 0.0 ) {
    gl_FragColor = vec4(gcu_RulerColor, 1.0);
  } else {
    discard;
  }
}
