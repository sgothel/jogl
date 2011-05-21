//Copyright 2010 JogAmp Community. All rights reserved.

#version 110

varying vec4    frontColor;

void main (void)
{
    gl_FragData[0] = vec4( frontColor.r, 0.0, 0.0, 1.0 ); 
    gl_FragData[1] = vec4( 0.0, frontColor.g, 0.0, 1.0 );
}
