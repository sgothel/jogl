        // Pass-2: AA on Texture
        // Note: gcv_FboTexCoord is in center of sample pixels.

        float sampleCount = gcu_FboTexSize.z;
        vec2 psize = 1.0 / gcu_FboTexSize.xy; // pixel size

        // Not only the poles (NW, SW, ..) but the whole edge!
        float pixelCount = sampleCount * sampleCount;

        // sampleCount [0, 1, 3, 5, 7] are undefined!
        float layerCount = ( sampleCount / 2.0 );

        // sum of all integer [layerCount .. 1] -> Gauss
        float denom = ( layerCount / 2.0 ) * ( layerCount + 1.0 );

        vec2 texCoord = gcv_FboTexCoord.st;

        vec4 t;

        // Layer-1: SampleCount 2 -> 4x
        t  = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-0.5, -0.5))); // NW
        t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-0.5,  0.5))); // SW
        t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 0.5,  0.5))); // SE
        t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 0.5, -0.5))); // NE

        t *= (layerCount - 0.0) / ( denom * 4.0 ); // weight layer 1

        if( sampleCount > 2.0 ) {
            // Layer-2: SampleCount 4 -> +12x = 16p
            vec4 tn = vec4(0);
            tn  = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-1.5, -1.5))); // NW -> SW Edge
            tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-1.5, -0.5)));
            tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-1.5,  0.5)));
            tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-1.5,  1.5))); // SW -> SE Edge
            tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-0.5,  1.5))); //
            tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 0.5,  1.5))); //
            tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 1.5,  1.5))); // SE -> NE Edge
            tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 1.5,  0.5))); // 
            tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 1.5, -0.5))); // 
            tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 1.5, -1.5))); // NE -> NW Edge
            tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 0.5, -1.5))); //
            tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-0.5, -1.5))); // NW - 1 (closed)

            t += tn * (layerCount - 1) / ( denom * 12.0 ); // weight layer 2

            if( sampleCount > 4.0 ) {
                // Layer-3: SampleCount 6 -> +20x = 36p
                tn  = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-2.5, -2.5))); // NW -> SW Edge
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-2.5, -1.5)));
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-2.5, -0.5)));
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-2.5,  0.5)));
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-2.5,  1.5)));
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-2.5,  2.5))); // SW -> SE Edge
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-1.5,  2.5)));
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-0.5,  2.5)));
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 0.5,  2.5)));
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 1.5,  2.5)));
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 2.5,  2.5))); // SE -> NE Edge
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 2.5,  1.5)));
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 2.5,  0.5)));
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 2.5, -0.5)));
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 2.5, -1.5)));
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 2.5, -2.5))); // NE -> NW Edge
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 1.5, -2.5)));
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 0.5, -2.5)));
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-0.5, -2.5)));
                tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-1.5, -2.5))); // NW - 1 (closed)

                t += tn * (layerCount - 2) / ( denom * 20.0 ); // weight layer 3

                if( sampleCount > 6.0 ) {
                    // Layer-4: SampleCount 8 -> +28x = 64p
                    tn  = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-3.5, -3.5))); // NW -> SW Edge
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-3.5, -2.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-3.5, -1.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-3.5, -0.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-3.5,  0.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-3.5,  1.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-3.5,  2.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-3.5,  3.5))); // SW -> SE Edge
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-2.5,  3.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-1.5,  3.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-0.5,  3.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 0.5,  3.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 1.5,  3.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 2.5,  3.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 3.5,  3.5))); // SE -> NE Edge
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 3.5,  2.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 3.5,  1.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 3.5,  0.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 3.5, -0.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 3.5, -1.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 3.5, -2.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 3.5, -3.5))); // NE -> NW Edge
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 2.5, -3.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 1.5, -3.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 0.5, -3.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-0.5, -3.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-1.5, -3.5)));
                    tn += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-2.5, -3.5))); // NW - 1 (closed)

                    t += tn * (layerCount - 3) / ( denom * 28.0 ); // weight layer 4
                }
            }
        }

        #if USE_DISCARD
            if( 0.0 == t.w ) {
                discard; // discard freezes NV tegra2 compiler
            } else {
                mgl_FragColor = t;
            }
        #else
            mgl_FragColor = t;
        #endif

