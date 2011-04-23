#ifndef precision_glsl
#define precision_glsl

#ifdef GL_ES
  #define MEDIUMP mediump
  #define HIGHP highp
  #define LOWP  lowp
#else
  #define MEDIUMP
  #define HIGHP
  #define LOWP
#endif

#endif // precision_glsl
