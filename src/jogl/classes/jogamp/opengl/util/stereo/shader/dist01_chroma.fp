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
varying vec2    svv_TexCoordG;
varying vec2    svv_TexCoordB;

void main (void)
{
  // 3 samples for fixing chromatic aberrations
  vec3 color = vec3(texture2D(svr_Texture0, svv_TexCoordR).r,
                    texture2D(svr_Texture0, svv_TexCoordG).g,
                    texture2D(svr_Texture0, svv_TexCoordB).b);
  svr_FragColor = vec4(svv_Fade * color, 1.0);  // include vignetteFade
}

