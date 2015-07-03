
        // Pass-2: AA on Texture
        // Note: gcv_FboTexCoord is in center of sample pixels, origin is bottom left!

        float sampleCount = gcu_FboTexSize.z;
        vec2 psize = 1.0 / gcu_FboTexSize.xy; // pixel size

        vec2 texCoord = gcv_FboTexCoord.st;
        float edge1Q = ( sampleCount / 2.0 ) - 1.0;

        vec4 t;
                                                                                           // SWIPE LEFT -> RIGHT
        t  = GetSample(gcu_FboTexUnit, texCoord, psize, -edge1Q,     0.0, -0.5,  0.5)*0.25; // upper-left  [p1]
        t += GetSample(gcu_FboTexUnit, texCoord, psize,     0.0, -edge1Q, -0.5, -0.5)*0.25; // lower-left  [p3]
        t += GetSample(gcu_FboTexUnit, texCoord, psize,     0.0,  edge1Q,  0.5,  0.5)*0.25; // upper-right [p2]
        t += GetSample(gcu_FboTexUnit, texCoord, psize,  edge1Q,     0.0,  0.5, -0.5)*0.25; // lower-right [p4]

        #if 0
        if(t.w == 0.0){
            discard; // discard freezes NV tegra2 compiler
        }
        #endif
        
        mgl_FragColor = t;

