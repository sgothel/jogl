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
  precision mediump float;
  precision mediump int;
#endif

varying   vec4    frontColor;

void main (void)
{
    gl_FragColor = frontColor;
}

