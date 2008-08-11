#ifndef es_precision_glsl
#define es_precision_glsl

#ifdef GL_ES
  #define MEDIUMP mediump
  #define HIGHP highp
#else
  #define MEDIUMP
  #define HIGHP
#endif

#endif
