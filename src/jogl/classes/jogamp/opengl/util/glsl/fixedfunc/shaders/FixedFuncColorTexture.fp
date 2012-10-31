
#if __VERSION__ >= 130
  #define varying in
  out vec4 mgl_FragColor;
  #define texture2D texture
#else
  #define mgl_FragColor gl_FragColor   
#endif


#include es_precision.glsl
#include mgl_lightdef.glsl

#include mgl_const.glsl
#include mgl_uniform.glsl
#include mgl_varying.glsl

#include mgl_alphatest.fp

const float gamma = 1.5; // FIXME
const vec3 igammav = vec3(1.0 / gamma); // FIXME
const vec4 texEnvColor = vec4(0.0); // FIXME

const vec4 zerov4 = vec4(0.0);
const vec4 onev4 = vec4(1.0);

void calcTexColor(inout vec4 color, vec4 texColor, in int texFormat, in int texEnvMode) {
    if(MGL_MODULATE == texEnvMode) { // default
        if( 4 == texFormat ) {
            color *= texColor;
        } else {
            color.rgb *= texColor.rgb;
        }
    } else if(MGL_REPLACE == texEnvMode) {
        if( 4 == texFormat ) {
            color = texColor;
        } else {
            color.rgb = texColor.rgb;
        }
    } else if(MGL_ADD == texEnvMode) {
        if( 4 == texFormat ) {
            color += texColor;
        } else {
            color.rgb += texColor.rgb;
        }
    } else if(MGL_BLEND == texEnvMode) {
        color.rgb = mix(color.rgb, texEnvColor.rgb, texColor.rgb);
        if( 4 == texFormat ) {
            color.a *= texColor.a;
        }
    } else if(MGL_DECAL == texEnvMode) {
        if( 4 == texFormat ) {
            color.rgb = mix(color.rgb, texColor.rgb, texColor.a);
        } else {
            color.rgb = texColor.rgb;
        }
    }
    color = clamp(color, zerov4, onev4);
}

void main (void)
{ 
  vec4 color = frontColor;

  /** ES2 supports CullFace implicit ..
  if( mgl_CullFace > 0 &&
      ( ( MGL_FRONT          == mgl_CullFace &&  gl_FrontFacing ) ||
        ( MGL_BACK           == mgl_CullFace && !gl_FrontFacing ) ||
        ( MGL_FRONT_AND_BACK == mgl_CullFace ) ) ) {
      DISCARD(color);
  } else { */
      #if MAX_TEXTURE_UNITS >= 2
      if( 0 != mgl_TextureEnabled[0] ) {
        calcTexColor(color, texture2D(mgl_Texture0, mgl_TexCoords[0].st), mgl_TexFormat[0], mgl_TexEnvMode[0]);
      }
      if( 0 != mgl_TextureEnabled[1] ) {
        calcTexColor(color, texture2D(mgl_Texture1, mgl_TexCoords[1].st), mgl_TexFormat[1], mgl_TexEnvMode[1]);
      }
      #endif
      #if MAX_TEXTURE_UNITS >= 4
      if( 0 != mgl_TextureEnabled[2] ) {
        calcTexColor(color, texture2D(mgl_Texture2, mgl_TexCoords[2].st), mgl_TexFormat[2], mgl_TexEnvMode[2]);
      }
      if( 0 != mgl_TextureEnabled[3] ) {
        calcTexColor(color, texture2D(mgl_Texture3, mgl_TexCoords[3].st), mgl_TexFormat[3], mgl_TexEnvMode[3]);
      }
      #endif
      #if MAX_TEXTURE_UNITS >= 8
      if( 0 != mgl_TextureEnabled[4] ) {
        calcTexColor(color, texture2D(mgl_Texture4, mgl_TexCoords[4].st), mgl_TexFormat[4], mgl_TexEnvMode[4]);
      }
      if( 0 != mgl_TextureEnabled[5] ) {
        calcTexColor(color, texture2D(mgl_Texture5, mgl_TexCoords[5].st), mgl_TexFormat[5], mgl_TexEnvMode[5]);
      }
      if( 0 != mgl_TextureEnabled[6] ) {
        calcTexColor(color, texture2D(mgl_Texture6, mgl_TexCoords[6].st), mgl_TexFormat[6], mgl_TexEnvMode[6]);
      }
      if( 0 != mgl_TextureEnabled[7] ) {
        calcTexColor(color, texture2D(mgl_Texture7, mgl_TexCoords[7].st), mgl_TexFormat[7], mgl_TexEnvMode[7]);
      }
      #endif
      if( mgl_AlphaTestFunc > 0 ) {
        alphaTest(color);
      }
  // } /* CullFace */

  mgl_FragColor = color;
  /**
  // simple alpha check
  if (color.a != 0.0) {
      mgl_FragColor = vec4(pow(color.rgb, igammav), color.a);
  } else {
      // discard; // freezes NV tegra2 compiler
      mgl_FragColor = color;
  } */
}

