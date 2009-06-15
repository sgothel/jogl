#ifndef es_precision_glsl
#define es_precision_glsl

#ifdef GL_ES
  #define MEDIUMP mediump
  #define HIGHP highp
  #define LOWP  lowp
#else
  #define MEDIUMP
  #define HIGHP
  #define LOWP
#endif

#endif // es_precision_glsl
