// Copyright 2010 JogAmp Community. All rights reserved.

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
    gl_FragColor = frontColor.bgra;
}

