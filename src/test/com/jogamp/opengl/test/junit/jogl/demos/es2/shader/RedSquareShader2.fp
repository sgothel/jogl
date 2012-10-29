// Copyright 2010 JogAmp Community. All rights reserved.

#if __VERSION__ >= 130
  #define varying in
  out vec4 mgl_FragColor;
#else
  #define mgl_FragColor gl_FragColor   
#endif

varying vec4 frontColor;

void main (void)
{
    mgl_FragColor = vec4(0.0, frontColor.g, frontColor.b, 1.0);
}

