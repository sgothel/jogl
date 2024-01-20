// Copyright 2023-2024 JogAmp Community. All rights reserved.

#ifndef varyings_glsl
#define varyings_glsl

varying vec3  gcv_CurveParam;

varying vec2  gcv_FboTexCoord;

#ifdef USE_COLOR_TEXTURE
    varying vec2  gcv_ColorTexCoord;
#endif
#ifdef USE_FRUSTUM_CLIPPING
    varying vec3  gcv_ClipCoord;
#endif    

#ifdef USE_COLOR_CHANNEL
    varying vec4  gcv_Color;
#endif

#endif // varyings_glsl

