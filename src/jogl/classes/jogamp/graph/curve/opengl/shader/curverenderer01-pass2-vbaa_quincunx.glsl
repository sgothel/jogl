
        // Pass-2: AA on Texture
        // Note: gcv_FboTexCoord is in center of sample pixels, origin is bottom left!

        float sampleCount = gcu_FboTexSize.z;
        vec2 psize = 1.0 / gcu_FboTexSize.xy; // pixel size

        // Just poles (NW, SW, ..)
        float edgeH = sampleCount / 2.0;

        vec2 texCoord = gcv_FboTexCoord.st;

        vec4 t;

        t  = GetSample(gcu_FboTexUnit, texCoord, psize,    0.0,    0.0, 0.0, 0.0)*0.5;   // w1 - center
        t += GetSample(gcu_FboTexUnit, texCoord, psize, -edgeH, -edgeH, 0.0, 0.0)*0.125; // w2 - sharing
        t += GetSample(gcu_FboTexUnit, texCoord, psize, -edgeH,  edgeH, 0.0, 0.0)*0.125; // w3 - edges
        t += GetSample(gcu_FboTexUnit, texCoord, psize,  edgeH, -edgeH, 0.0, 0.0)*0.125; // w4 - w/ all pixels
        t += GetSample(gcu_FboTexUnit, texCoord, psize,  edgeH,  edgeH, 0.0, 0.0)*0.125; // w5

        #if USE_DISCARD
            if( 0.0 == t.w ) {
                discard; // discard freezes NV tegra2 compiler
            } else {
                mgl_FragColor = t;
            }
        #else
            mgl_FragColor = t;
        #endif

