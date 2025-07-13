// Copyright 2025 JogAmp Community. All rights reserved.

#if __VERSION__ >= 130
  #define varying in
  out vec4 mgl_FragColor;
#else
  #define mgl_FragColor gl_FragColor
#endif

varying vec3 gcv_FillColor;

void main()
{
    mgl_FragColor = vec4(gcv_FillColor, 1);
}