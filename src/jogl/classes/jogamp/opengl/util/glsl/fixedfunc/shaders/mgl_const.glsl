
#ifndef mgl_const_glsl
#define mgl_const_glsl

#include es_precision.glsl

// will be defined at runtime: MAX_TEXTURE_UNITS [0|2|4|8]
const   LOWP int     MAX_LIGHTS = 8; 

const        float   EPSILON =  0.0000001;  // FIXME: determine proper hw-precision

// discard freezes NV tegra2 compiler (STILL TRUE?)
// #define DISCARD(c) (c.a = 0.0)
#define DISCARD(c) discard

// Texture Environment / Multi Texturing
#define MGL_ADD      1
#define MGL_MODULATE 2
#define MGL_DECAL    3
#define MGL_BLEND    4
#define MGL_REPLACE  5
#define MGL_COMBINE  6

// Alpha Test
#define MGL_NEVER    1
#define MGL_LESS     2
#define MGL_EQUAL    3
#define MGL_LEQUAL   4
#define MGL_GREATER  5
#define MGL_NOTEQUAL 6
#define MGL_GEQUAL   7
#define MGL_ALWAYS   8

// Cull Face
#define MGL_FRONT            1
#define MGL_BACK             2
#define MGL_FRONT_AND_BACK   3

#endif // mgl_const_glsl
