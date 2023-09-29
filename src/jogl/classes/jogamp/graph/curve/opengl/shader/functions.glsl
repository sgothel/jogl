// Copyright 2023 JogAmp Community. All rights reserved.

#ifndef functions_glsl
#define functions_glsl

/** Returns product of components. */
float v_mul( vec2 v ) {
    return v.x * v.y;
}
/** Returns component wise logical 'or' as float '0' or '1' using the components product and clamp. */ 
float v_or( vec2 v ) {
    return clamp(v.x * v.y, 0, 1);
}

/** Returns sum of components. */
float v_sum( vec2 v ) {
    return v.x + v.y;
}
/** Returns component wise logical 'and' as float '0' or '1' using the components sum  and clamp. */
float v_and( vec2 v ) {
    return clamp(v.x + v.y, 0, 1);
}

/**
 * Branch-less clipping function.
 * <p>
 * Returns either 'col_in' if the 'coord' is within ['low'..'high'] range,
 * otherwise 'col_ex'.
 * </p>
 * <p>
 * This is achieved via the build-in 'step' and 'mix' function 
 * as well as our own 'v_mul' and v_and' function, 
 * which flattens a 'vec2' to one float suitable to be used as the 'mix' criteria.
 * </p>
 */
vec4 clip_coord(vec4 col_in, vec4 col_ex, vec2 coord, vec2 low, vec2 high) {
    vec4 c = mix( col_ex, col_in, v_mul( step(low,  coord) ));
    return   mix( c,      col_ex, v_and( step(high, coord) ));    
}

#endif // functions_glsl

