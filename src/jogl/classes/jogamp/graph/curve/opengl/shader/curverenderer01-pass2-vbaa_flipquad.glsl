
        // Pass-2: AA on Texture
        // Note: gcv_FboTexCoord is in center of sample pixels, origin is bottom left!

        float sampleCount = gcu_FboTexSize.z;
        vec2 psize = 1.0 / gcu_FboTexSize.xy; // pixel size

        // Just poles (NW, SW, ..)
        float edge1H = sampleCount / 2.0;
        float edge1T = sampleCount / 3.0;
        float edgeTH = edge1H-edge1T;

        vec2 normFragCoord = gl_FragCoord.xy - vec2(0.5, 0.5); // normalize center 0.5/0.5 -> 0/0
        vec2 modPos = mod(normFragCoord, 2.0);
        float orient = mod(modPos.x + modPos.y, 2.0); // mirrored on all odd columns, alternating each row (checker-board pattern)

        vec2 texCoord = gcv_FboTexCoord.st;
        vec4 t;

        if( 0.0 == orient ) {
                                                                                               // SWIPE LEFT -> RIGHT
            t  = GetSample(gcu_FboTexUnit, texCoord, psize, -edge1H,  edgeTH, 0.0, 0.0)*0.25; // upper-left  [p1]
            t += GetSample(gcu_FboTexUnit, texCoord, psize, -edgeTH, -edge1H, 0.0, 0.0)*0.25; // lower-left  [p3]
            t += GetSample(gcu_FboTexUnit, texCoord, psize,  edgeTH,  edge1H, 0.0, 0.0)*0.25; // upper-right [p2]
            t += GetSample(gcu_FboTexUnit, texCoord, psize,  edge1H, -edgeTH, 0.0, 0.0)*0.25; // lower-right [p4]
        } else {
            t  = GetSample(gcu_FboTexUnit, texCoord, psize, -edge1H, -edgeTH, 0.0, 0.0)*0.25; // lower-left  [p4]
            t += GetSample(gcu_FboTexUnit, texCoord, psize, -edgeTH,  edge1H, 0.0, 0.0)*0.25; // upper-left  [p3]
            t += GetSample(gcu_FboTexUnit, texCoord, psize,  edgeTH, -edge1H, 0.0, 0.0)*0.25; // lower-right [p2]
            t += GetSample(gcu_FboTexUnit, texCoord, psize,  edge1H,  edgeTH, 0.0, 0.0)*0.25; // upper-right [p1]
        }


        #if 0
        if(t.w == 0.0){
            discard; // discard freezes NV tegra2 compiler
        }
        #endif
        
        mgl_FragColor = t;

