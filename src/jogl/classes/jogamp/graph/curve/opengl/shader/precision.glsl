#ifndef precision_glsl
#define precision_glsl

#ifdef GL_ES
  #define MEDIUMP mediump
  #define HIGHP highp
  #define LOWP  lowp
  #define GRAPHP mediump
#else
  #define MEDIUMP
  #define HIGHP
  #define LOWP
  #define GRAPHP
#endif

#endif // precision_glsl
