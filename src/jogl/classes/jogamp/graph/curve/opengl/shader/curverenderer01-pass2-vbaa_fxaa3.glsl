
        // Pass-2: AA on Texture
        // Note: gcv_FboTexCoord is in center of sample pixels.

        if( gcu_FboTexSize.z < 4.0 ) {
            // FXAA-2
            const float FXAA_REDUCE_MIN  = (1.0/128.0);
            const float FXAA_REDUCE_MUL  = (1.0/8.0);
            const float FXAA_SPAN_MAX  = 8.0;

            float sampleCount = gcu_FboTexSize.z;

            vec2 texCoord = gcv_FboTexCoord.st;
            const float poff = 1.0;
            vec2 psize = 1.0 / texCoord; // pixel size

            vec3 rgbNW = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-poff, -poff))).rgb;
            vec3 rgbSW = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-poff,  poff))).rgb;
            vec3 rgbSE = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( poff,  poff))).rgb;
            vec3 rgbNE = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( poff, -poff))).rgb;
            vec4 rgbM  = texture2D(gcu_FboTexUnit, texCoord);

            const vec3 luma = vec3(0.299, 0.587, 0.114);
            float lumaNW = dot(rgbNW, luma);
            float lumaNE = dot(rgbNE, luma);
            float lumaSW = dot(rgbSW, luma);
            float lumaSE = dot(rgbSE, luma);
            float lumaM = dot(rgbM.rgb, luma);
            float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
            float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));
            vec2 dir;
            dir.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));
            dir.y = ((lumaNW + lumaSW) - (lumaNE + lumaSE));
            float dirReduce = max((lumaNW + lumaNE + lumaSW + lumaSE) * (0.25 * FXAA_REDUCE_MUL),FXAA_REDUCE_MIN);
            float rcpDirMin = 1.0/(min(abs(dir.x), abs(dir.y)) + dirReduce);
            dir = min( vec2( FXAA_SPAN_MAX, FXAA_SPAN_MAX),
                       max( vec2(-FXAA_SPAN_MAX, -FXAA_SPAN_MAX),dir * rcpDirMin) ) * psize;
                       

            vec3 rgbA = 0.5 * ( texture2D(gcu_FboTexUnit, texCoord + dir * (1.0/3.0 - 0.5)).rgb + 
                                texture2D(gcu_FboTexUnit, texCoord + dir * (2.0/3.0 - 0.5)).rgb );
            vec3 rgbB = rgbA * 0.5 + 0.25 * ( texture2D(gcu_FboTexUnit, texCoord + (dir * - 0.5)).rgb + 
                                              texture2D(gcu_FboTexUnit, texCoord + (dir *   0.5)).rgb );
            float lumaB = dot(rgbB, luma);
            if((lumaB < lumaMin) || (lumaB > lumaMax)) {
                mgl_FragColor.rgb = rgbA;
            } else {
                mgl_FragColor.rgb = rgbB;
            }
            mgl_FragColor.a = gcu_Alpha * rgbM.a; // mix(0.0, gcu_Alpha, rgbM.a); // t.a one of [ 0.0, 1.0 ]
        } else {

#include curverenderer01-pass2-vbaa_poles_equalweight.glsl

        }

