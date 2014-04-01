
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

        #if 0
        if(t.w == 0.0){
            discard; // discard freezes NV tegra2 compiler
        }
        #endif
        
        mgl_FragColor = t;

