        // Pass-2: AA on Texture
        // Note: gcv_FboTexCoord is in center of sample pixels.

        vec2 texCoord = gcv_FboTexCoord.st;

        float sampleCount = gcu_FboTexSize.z;
        vec2 tsize = gcu_FboTexSize.xy; // tex size
        vec2 psize = 1.0 / gcu_FboTexSize.xy; // pixel size

        // mix(x,y,a): x*(1-a) + y*a
        //
        // bilinear filtering includes 2 mix:
        //
        //   pix1 = tex[x0][y0] * ( 1 - u_ratio ) + tex[x1][y0] * u_ratio
        //   pix2 = tex[x0][y1] * ( 1 - u_ratio ) + tex[x1][y1] * u_ratio
        //   fin  =    pix1     * ( 1 - v_ratio ) +     pix2    * v_ratio
        //
        // so we can use the build in mix function for these 2 computations ;-)
        //
        vec2 uv_ratio     = fract(texCoord*tsize); // texCoord*tsize - floor(texCoord*tsize);

        // Just poles (NW, SW, ..)
        float pixelCount = 2 * sampleCount;

        // sampleCount [0, 1, 3, 5, 7] are undefined!
        float layerCount = ( sampleCount / 2.0 );

        // sum of all integer [layerCount .. 1] -> Gauss
        float denom = ( layerCount / 2.0 ) * ( layerCount + 1.0 );

        vec4 t, p1, p2, p3, p4;

        // Layer-1: SampleCount 2 -> 4x
        p1 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-0.5, -0.5))); // NW
        p2 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-0.5,  0.5))); // SW
        p3 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 0.5,  0.5))); // SE
        p4 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 0.5, -0.5))); // NE

        p1  = mix( p1, p4, uv_ratio.x);
        p2  = mix( p2, p3, uv_ratio.x);
        t  = mix ( p1, p2, uv_ratio.y );

        t *= (layerCount - 0.0) / ( denom ); // weight layer 1

        if( sampleCount > 2.0 ) {
            // Layer-2: SampleCount 4 -> +4x = 8p
            p1 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-1.5, -1.5))); // NW
            p2 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-1.5,  1.5))); // SW
            p3 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 1.5,  1.5))); // SE
            p4 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 1.5, -1.5))); // NE

            p1  = mix( p1, p4, uv_ratio.x);
            p2  = mix( p2, p3, uv_ratio.x);
            p3  = mix ( p1, p2, uv_ratio.y );
            t += p3 * (layerCount - 1) / ( denom ); // weight layer 2

            if( sampleCount > 4.0 ) {
                // Layer-3: SampleCount 6 -> +4 = 12p
                p1 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-2.5, -2.5))); // NW
                p2 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-2.5,  2.5))); // SW
                p3 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 2.5,  2.5))); // SE
                p4 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 2.5, -2.5))); // NE

                p1  = mix( p1, p4, uv_ratio.x);
                p2  = mix( p2, p3, uv_ratio.x);
                p3  = mix ( p1, p2, uv_ratio.y );
                t += p3 * (layerCount - 2) / ( denom ); // weight layer 3

                if( sampleCount > 6.0 ) {
                    // Layer-4: SampleCount 8 -> +4 = 16p
                    p1 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-3.5, -3.5))); // NW
                    p2 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2(-3.5,  3.5))); // SW
                    p3 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 3.5,  3.5))); // SE
                    p4 = texture2D(gcu_FboTexUnit, texCoord + psize*(vec2( 3.5, -3.5))); // NE

                    p1  = mix( p1, p4, uv_ratio.x);
                    p2  = mix( p2, p3, uv_ratio.x);
                    p3  = mix ( p1, p2, uv_ratio.y );

                    t += p3 * (layerCount - 3) / ( denom ); // weight layer 4
                }
            }
        }

        #if 0
        if(t.w == 0.0){
            discard; // discard freezes NV tegra2 compiler
        }
        #endif
        
        mgl_FragColor = t;
