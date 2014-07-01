//Copyright 2014 JogAmp Community. All rights reserved.

#if __VERSION__ >= 130
  #define varying in
  out vec4 ovr_FragColor;
  #define texture2D texture
#else
  #define ovr_FragColor gl_FragColor
#endif

uniform sampler2D  ovr_Texture0;

varying vec3    ovv_Fade;
varying vec2    ovv_TexCoordR;
varying vec2    ovv_TexCoordG;
varying vec2    ovv_TexCoordB;

void main (void)
{
  // 3 samples for fixing chromatic aberrations
  vec3 color = vec3(texture2D(ovr_Texture0, ovv_TexCoordR).r,
                    texture2D(ovr_Texture0, ovv_TexCoordG).g,
                    texture2D(ovr_Texture0, ovv_TexCoordB).b);
  ovr_FragColor = vec4(ovv_Fade * color, 1.0);  // include vignetteFade
}

