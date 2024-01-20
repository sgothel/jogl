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

        // SampleCount 2 -> 4x
        t  = texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-0.5, -0.5))*sample_weight; // NW
        t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-0.5,  0.5))*sample_weight; // SW
        t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 0.5,  0.5))*sample_weight; // SE
        t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 0.5, -0.5))*sample_weight; // NE
        #if SAMPLE_COUNT > 2
            // SampleCount 4 -> +12x = 16p
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-1.5, -1.5))*sample_weight; // NW -> SW Edge
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-1.5, -0.5))*sample_weight;
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-1.5,  0.5))*sample_weight;
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-1.5,  1.5))*sample_weight; // SW -> SE Edge
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-0.5,  1.5))*sample_weight; //
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 0.5,  1.5))*sample_weight; //
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 1.5,  1.5))*sample_weight; // SE -> NE Edge
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 1.5,  0.5))*sample_weight; // 
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 1.5, -0.5))*sample_weight; // 
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 1.5, -1.5))*sample_weight; // NE -> NW Edge
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 0.5, -1.5))*sample_weight; //
            t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-0.5, -1.5))*sample_weight; // NW - 1 (closed)

            #if SAMPLE_COUNT > 4
                // SampleCount 6 -> +20x = 36p
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-2.5, -2.5))*sample_weight; // NW -> SW Edge
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-2.5, -1.5))*sample_weight;
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-2.5, -0.5))*sample_weight;
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-2.5,  0.5))*sample_weight;
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-2.5,  1.5))*sample_weight;
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-2.5,  2.5))*sample_weight; // SW -> SE Edge
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-1.5,  2.5))*sample_weight;
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-0.5,  2.5))*sample_weight;
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 0.5,  2.5))*sample_weight;
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 1.5,  2.5))*sample_weight;
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 2.5,  2.5))*sample_weight; // SE -> NE Edge
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 2.5,  1.5))*sample_weight;
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 2.5,  0.5))*sample_weight;
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 2.5, -0.5))*sample_weight;
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 2.5, -1.5))*sample_weight;
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 2.5, -2.5))*sample_weight; // NE -> NW Edge
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 1.5, -2.5))*sample_weight;
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 0.5, -2.5))*sample_weight;
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-0.5, -2.5))*sample_weight;
                t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-1.5, -2.5))*sample_weight; // NW - 1 (closed)
                #if SAMPLE_COUNT > 6
                    // SampleCount 8 -> +28x = 64p
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-3.5, -3.5))*sample_weight; // NW -> SW Edge
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-3.5, -2.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-3.5, -1.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-3.5, -0.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-3.5,  0.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-3.5,  1.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-3.5,  2.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-3.5,  3.5))*sample_weight; // SW -> SE Edge
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-2.5,  3.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-1.5,  3.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-0.5,  3.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 0.5,  3.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 1.5,  3.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 2.5,  3.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 3.5,  3.5))*sample_weight; // SE -> NE Edge
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 3.5,  2.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 3.5,  1.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 3.5,  0.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 3.5, -0.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 3.5, -1.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 3.5, -2.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 3.5, -3.5))*sample_weight; // NE -> NW Edge
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 2.5, -3.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 1.5, -3.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2( 0.5, -3.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-0.5, -3.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-1.5, -3.5))*sample_weight;
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*vec2(-2.5, -3.5))*sample_weight; // NW - 1 (closed)
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

