
#ifndef attributes_glsl
#define attributes_glsl

#if __VERSION__ >= 130
  #define attribute in
#endif

// attribute vec3    gca_Vertices;
attribute vec4    gca_Vertices;
attribute vec2    gca_TexCoords;
//attribute vec4    gca_Colors;
//attribute vec3    gca_Normals;

#endif // attributes_glsl
