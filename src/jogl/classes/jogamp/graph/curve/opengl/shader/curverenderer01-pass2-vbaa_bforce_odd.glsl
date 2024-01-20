    // Pass-2: AA on Texture
    // Note: gcv_FboTexCoord is in center of sample pixels.

#ifdef USE_FRUSTUM_CLIPPING
    if( isOutsideMvFrustum(gcv_ClipCoord) ) {
        #if USE_DISCARD
            discard; // discard freezes NV tegra2 compiler
        #else
            mgl_FragColor = vec4(0);
        #endif
    } else
#endif
    {     
        // float sample_count = gcu_FboTexSize.z;
        vec2 psize = 1.0 / gcu_FboTexSize.xy; // pixel size

        // Not only the poles (NW, SW, ..) but the whole edge!
        const float sample_weight = 1.0 / ( sample_count * sample_count );

        // const vec4 tex_weights = vec4(0.075, 0.06, 0.045, 0.025);
        vec2 texCoord = gcv_FboTexCoord.st;

        vec4 t;

        // SampleCount 1 -> 1x (raster 1x1)
        t += texture2D(gcu_FboTexUnit, texCoord)*sample_weight; // CENTER

        #if SAMPLE_COUNT > 1
            // SampleCount 3 -> +8x = 9p (raster 3x3)
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-1.0, -1.0))*sample_weight; // top line
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 0.0, -1.0))*sample_weight; // top line
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 1.0, -1.0))*sample_weight; // top line
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-1.0,  0.0))*sample_weight; // Center Left
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 1.0,  0.0))*sample_weight; // Center Right
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-1.0,  1.0))*sample_weight; // bottom line
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 0.0,  1.0))*sample_weight; // bottom line
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 1.0,  1.0))*sample_weight; // bottom line

            #if SAMPLE_COUNT > 3
                // SampleCount 5 -> +16x = 25p (raster 5x5)
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-2.0, -1.0))*sample_weight; // top line - 1
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 2.0, -1.0))*sample_weight; // top line - 1
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-2.0,  0.0))*sample_weight; // Center Left
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 2.0,  0.0))*sample_weight; // Center Right
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-2.0,  1.0))*sample_weight; // bottom line - 1
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 2.0,  1.0))*sample_weight; // bottom line - 1

                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-2.0, -2.0))*sample_weight; // top line
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-1.0, -2.0))*sample_weight; // top line
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 0.0, -2.0))*sample_weight; // top line
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 1.0, -2.0))*sample_weight; // top line
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 2.0, -2.0))*sample_weight; // top line

                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-2.0,  2.0))*sample_weight; // bottom line
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-1.0,  2.0))*sample_weight; // bottom line
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 0.0,  2.0))*sample_weight; // bottom line
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 1.0,  2.0))*sample_weight; // bottom line
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 2.0,  2.0))*sample_weight; // bottom line

                #if SAMPLE_COUNT > 5
                    // SampleCount 7 -> +24x = 49p (raster 7x7)
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-3.0, -2.0))*sample_weight; // top line - 1
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 3.0, -2.0))*sample_weight; // top line - 1
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-3.0, -1.0))*sample_weight; // top line - 2
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 3.0, -1.0))*sample_weight; // top line - 2
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-3.0,  0.0))*sample_weight; // Center Left
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 3.0,  0.0))*sample_weight; // Center Right
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-3.0,  1.0))*sample_weight; // bottom line - 2
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 3.0,  1.0))*sample_weight; // bottom line - 2
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-3.0,  2.0))*sample_weight; // bottom line - 1
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 3.0,  2.0))*sample_weight; // bottom line - 1

                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-3.0, -3.0))*sample_weight; // top line
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-2.0, -3.0))*sample_weight; // top line
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-1.0, -3.0))*sample_weight; // top line
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 0.0, -3.0))*sample_weight; // top line
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 1.0, -3.0))*sample_weight; // top line
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 2.0, -3.0))*sample_weight; // top line
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 3.0, -3.0))*sample_weight; // top line

                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-3.0,  3.0))*sample_weight; // bottom line
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-2.0,  3.0))*sample_weight; // bottom line
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-1.0,  3.0))*sample_weight; // bottom line
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 0.0,  3.0))*sample_weight; // bottom line
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 1.0,  3.0))*sample_weight; // bottom line
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 2.0,  3.0))*sample_weight; // bottom line
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 3.0,  3.0))*sample_weight; // bottom line
                #endif
            #endif
        #endif

        #if USE_DISCARD
            if( 0.0 == t.w ) {
                discard; // discard freezes NV tegra2 compiler
            } else {
                mgl_FragColor = t;
            }
        #else
            mgl_FragColor = t;
        #endif
    }     

