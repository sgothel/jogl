//Copyright 2010 JogAmp Community. All rights reserved.

#if __VERSION__ >= 130
  #define varying in
  out vec4 mgl_FragData[2];
#else
  #define mgl_FragData gl_FragData   
#endif

varying vec4    frontColor;

void main (void)
{
    mgl_FragData[0] = vec4( frontColor.r, 0.0, 0.0, 1.0 ); 
    mgl_FragData[1] = vec4( 0.0, frontColor.g, 0.0, 1.0 );
}
