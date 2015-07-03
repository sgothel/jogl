
        // Pass-2: AA on Texture
        // Note: gcv_FboTexCoord is in center of sample pixels.

        float sampleCount = gcu_FboTexSize.z;
        vec2 psize = 1.0 / gcu_FboTexSize.xy; // pixel size

        // Just poles (NW, SW, ..)
        float sample_weight = 1 / ( 2 * sampleCount );

        vec2 texCoord = gcv_FboTexCoord.st;

        vec4 t;

        // SampleCount 2 -> 4x
        t  = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-0.5, -0.5)))*sample_weight; // NW
        t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-0.5,  0.5)))*sample_weight; // SW
        t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 0.5,  0.5)))*sample_weight; // SE
        t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 0.5, -0.5)))*sample_weight; // NE
        if( sampleCount > 2.0 ) {
            // SampleCount 4 -> +4 = 8p
            t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-1.5, -1.5)))*sample_weight; // NW
            t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-1.5,  1.5)))*sample_weight; // SW
            t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 1.5,  1.5)))*sample_weight; // SE
            t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 1.5, -1.5)))*sample_weight; // NE

            if( sampleCount > 4.0 ) {
                // SampleCount 6 -> +4 = 12p
                t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-2.5, -2.5)))*sample_weight; // NW
                t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-2.5,  2.5)))*sample_weight; // SW
                t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 2.5,  2.5)))*sample_weight; // SE
                t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 2.5, -2.5)))*sample_weight; // NE
                if( sampleCount > 6.0 ) {
                    // SampleCount 8 -> +4 = 16p
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-3.5, -3.5)))*sample_weight; // NW
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-3.5,  3.5)))*sample_weight; // SW
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 3.5,  3.5)))*sample_weight; // SE
                    t += texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 3.5, -3.5)))*sample_weight; // NE
                }
            }
        }
        #if 0
        if(t.w == 0.0){
            discard; // discard freezes NV tegra2 compiler
        }
        #endif
        
        mgl_FragColor = t;

