
        // Pass-2: AA on Texture
        // Note: gcv_TexCoord is in center of sample pixels.

        float sampleCount = gcu_TextureSize.z;
        vec2 psize = 1.0 / gcu_TextureSize.xy; // pixel size

        // Just poles (NW, SW, ..)
        float sample_weight = 1 / ( 2 * sampleCount );

        vec2 texCoord = gcv_TexCoord.st;

        vec4 t;

        // SampleCount 2 -> 4x
        t  = texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-0.5, -0.5)))*sample_weight; // NW
        t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-0.5,  0.5)))*sample_weight; // SW
        t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 0.5,  0.5)))*sample_weight; // SE
        t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 0.5, -0.5)))*sample_weight; // NE
        if( sampleCount > 2.0 ) {
            // SampleCount 4 -> +4 = 8p
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-1.5, -1.5)))*sample_weight; // NW
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-1.5,  1.5)))*sample_weight; // SW
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 1.5,  1.5)))*sample_weight; // SE
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 1.5, -1.5)))*sample_weight; // NE

            if( sampleCount > 4.0 ) {
                // SampleCount 6 -> +4 = 12p
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-2.5, -2.5)))*sample_weight; // NW
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-2.5,  2.5)))*sample_weight; // SW
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 2.5,  2.5)))*sample_weight; // SE
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 2.5, -2.5)))*sample_weight; // NE
                if( sampleCount > 6.0 ) {
                    // SampleCount 8 -> +4 = 16p
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-3.5, -3.5)))*sample_weight; // NW
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-3.5,  3.5)))*sample_weight; // SW
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 3.5,  3.5)))*sample_weight; // SE
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 3.5, -3.5)))*sample_weight; // NE
                }
            }
        }
        #if 0
        if(t.w == 0.0){
            discard; // discard freezes NV tegra2 compiler
        }
        #endif
        
        color = t.rgb;
        alpha = gcu_Alpha * t.a;

