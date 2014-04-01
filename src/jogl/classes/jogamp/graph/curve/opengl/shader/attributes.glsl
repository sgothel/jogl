
#ifndef attributes_glsl
#define attributes_glsl

// attribute vec3    gca_Vertices;
attribute vec4    gca_Vertices;

/**
 * CDTriangulator2D.extractBoundaryTriangles(..):
 *     AA line (exp)   : z > 0
 *     line            : x ==   0, y == 0
 *     hole or holeLike: 0 > y
 *     !hole           : 0 < y 
 *      
 *     0   == gcv_CurveParams.x : vertex-0 of triangle
 *     0.5 == gcv_CurveParams.x : vertex-1 of triangle
 *     1   == gcv_CurveParams.x : vertex-2 of triangle
 */
attribute vec3    gca_CurveParams;

attribute vec4    gca_FboVertices;
attribute vec2    gca_FboTexCoords;

#ifdef USE_COLOR_CHANNEL
    attribute vec4    gca_Colors;
#endif

//attribute vec3    gca_Normals;

#endif // attributes_glsl
