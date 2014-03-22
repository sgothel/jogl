
        // Pass-2: AA on Texture
        // Note: gcv_TexCoord is in center of sample pixels.

        if( gcu_TextureSize.z < 4 ) {
            // FXAA-2
            const float FXAA_REDUCE_MIN  = (1.0/128.0);
            const float FXAA_REDUCE_MUL  = (1.0/8.0);
            const float FXAA_SPAN_MAX  = 8.0;

            const float sampleCount = gcu_TextureSize.z;

            const vec2 texCoord = gcv_TexCoord.st;
            const float poff = 1.0;
            const vec2 psize = 1.0 / texCoord; // pixel size

            const vec3 rgbNW = texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-poff, -poff))).rgb;
            const vec3 rgbSW = texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-poff,  poff))).rgb;
            const vec3 rgbSE = texture2D(gcu_TextureUnit, texCoord + psize*(vec2( poff,  poff))).rgb;
            const vec3 rgbNE = texture2D(gcu_TextureUnit, texCoord + psize*(vec2( poff, -poff))).rgb;
            const vec4 rgbM  = texture2D(gcu_TextureUnit, texCoord);

            const vec3 luma = vec3(0.299, 0.587, 0.114);
            const float lumaNW = dot(rgbNW, luma);
            const float lumaNE = dot(rgbNE, luma);
            const float lumaSW = dot(rgbSW, luma);
            const float lumaSE = dot(rgbSE, luma);
            const float lumaM = dot(rgbM.rgb, luma);
            const float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
            const float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));
            vec2 dir;
            dir.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));
            dir.y = ((lumaNW + lumaSW) - (lumaNE + lumaSE));
            const float dirReduce = max((lumaNW + lumaNE + lumaSW + lumaSE) * (0.25 * FXAA_REDUCE_MUL),FXAA_REDUCE_MIN);
            const float rcpDirMin = 1.0/(min(abs(dir.x), abs(dir.y)) + dirReduce);
            dir = min( vec2( FXAA_SPAN_MAX, FXAA_SPAN_MAX),
                       max( vec2(-FXAA_SPAN_MAX, -FXAA_SPAN_MAX),dir * rcpDirMin) ) * psize;
                       

            const vec3 rgbA = 0.5 * ( texture2D(gcu_TextureUnit, texCoord + dir * (1.0/3.0 - 0.5)).rgb + 
                                      texture2D(gcu_TextureUnit, texCoord + dir * (2.0/3.0 - 0.5)).rgb );
            const vec3 rgbB = rgbA * 0.5 + 0.25 * ( texture2D(gcu_TextureUnit, texCoord + (dir * - 0.5)).rgb + 
                                                    texture2D(gcu_TextureUnit, texCoord + (dir *   0.5)).rgb );
            const float lumaB = dot(rgbB, luma);
            if((lumaB < lumaMin) || (lumaB > lumaMax)) {
                color = rgbA;
            } else {
                color = rgbB;
            }
            alpha = gcu_Alpha * rgbM.a; // mix(0.0, gcu_Alpha, rgbM.a); // t.a one of [ 0.0, 1.0 ]
        } else {

#include curverenderer01-pass2-vbaa_poles_equalweight.glsl

        }

