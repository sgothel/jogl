//Copyright 2014 JogAmp Community. All rights reserved.

#if __VERSION__ >= 130
  #define varying in
  out vec4 svr_FragColor;
  #define texture2D texture
#else
  #define svr_FragColor gl_FragColor
#endif

uniform sampler2D  svr_Texture0;

varying vec3    svv_Fade;
varying vec2    svv_TexCoordR;

void main (void)
{
  // 3 samples for fixing chromatic aberrations
  vec3 color = texture2D(svr_Texture0, svv_TexCoordR).rgb;
  svr_FragColor = vec4(svv_Fade * color, 1.0);  // include vignetteFade
}

