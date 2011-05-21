// Copyright 2010 JogAmp Community. All rights reserved.

/**
 * AMD complains: #version must occur before any other statement in the program
#ifdef GL_ES
    #version 100
#else
    #version 110
#endif
 */

#ifdef GL_ES
  #define MEDIUMP mediump
  #define HIGHP highp
#else
  #define MEDIUMP
  #define HIGHP
#endif

varying   HIGHP vec4    frontColor;

void main (void)
{
    gl_FragColor = vec4(0.0, frontColor.g, frontColor.b, 1.0);
}

