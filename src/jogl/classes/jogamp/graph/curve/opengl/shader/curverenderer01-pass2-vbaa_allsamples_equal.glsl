        // Pass-2: AA on Texture
        // Note: gcv_TexCoord is in center of sample pixels.

        const float sampleCount = gcu_TextureSize.z;
        const vec2 psize = 1.0 / gcu_TextureSize.xy; // pixel size

        // Not only the poles (NW, SW, ..) but the whole edge!
        const float sample_weight = 1 / ( sampleCount * sampleCount );

        // const vec4 tex_weights = vec4(0.075, 0.06, 0.045, 0.025);

        const vec2 texCoord = gcv_TexCoord.st;

        vec4 t;

        // SampleCount 2 -> 4x
        t  = texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-0.5, -0.5)))*sample_weight; // NW
        t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-0.5,  0.5)))*sample_weight; // SW
        t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 0.5,  0.5)))*sample_weight; // SE
        t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 0.5, -0.5)))*sample_weight; // NE
        if( sampleCount > 2 ) {
            // SampleCount 4 -> +12x = 16p
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-1.5, -1.5)))*sample_weight; // NW -> SW Edge
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-1.5, -0.5)))*sample_weight;
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-1.5,  0.5)))*sample_weight;
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-1.5,  1.5)))*sample_weight; // SW -> SE Edge
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-0.5,  1.5)))*sample_weight; //
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 0.5,  1.5)))*sample_weight; //
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 1.5,  1.5)))*sample_weight; // SE -> NE Edge
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 1.5,  0.5)))*sample_weight; // 
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 1.5, -0.5)))*sample_weight; // 
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 1.5, -1.5)))*sample_weight; // NE -> NW Edge
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 0.5, -1.5)))*sample_weight; //
            t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-0.5, -1.5)))*sample_weight; // NW - 1 (closed)

            if( sampleCount > 4 ) {
                // SampleCount 6 -> +20x = 36p
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-2.5, -2.5)))*sample_weight; // NW -> SW Edge
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-2.5, -1.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-2.5, -0.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-2.5,  0.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-2.5,  1.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-2.5,  2.5)))*sample_weight; // SW -> SE Edge
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-1.5,  2.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-0.5,  2.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 0.5,  2.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 1.5,  2.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 2.5,  2.5)))*sample_weight; // SE -> NE Edge
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 2.5,  1.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 2.5,  0.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 2.5, -0.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 2.5, -1.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 2.5, -2.5)))*sample_weight; // NE -> NW Edge
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 1.5, -2.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 0.5, -2.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-0.5, -2.5)))*sample_weight;
                t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-1.5, -2.5)))*sample_weight; // NW - 1 (closed)
                if( sampleCount > 6 ) {
                    // SampleCount 8 -> +28x = 64p
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-3.5, -3.5)))*sample_weight; // NW -> SW Edge
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-3.5, -2.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-3.5, -1.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-3.5, -0.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-3.5,  0.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-3.5,  1.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-3.5,  2.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-3.5,  3.5)))*sample_weight; // SW -> SE Edge
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-2.5,  3.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-1.5,  3.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-0.5,  3.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 0.5,  3.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 1.5,  3.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 2.5,  3.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 3.5,  3.5)))*sample_weight; // SE -> NE Edge
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 3.5,  2.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 3.5,  1.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 3.5,  0.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 3.5, -0.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 3.5, -1.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 3.5, -2.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 3.5, -3.5)))*sample_weight; // NE -> NW Edge
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 2.5, -3.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 1.5, -3.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2( 0.5, -3.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-0.5, -3.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-1.5, -3.5)))*sample_weight;
                    t += texture2D(gcu_TextureUnit, texCoord + psize*(vec2(-2.5, -3.5)))*sample_weight; // NW - 1 (closed)
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
