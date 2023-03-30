
        // Pass-2: AA on Texture
        // Note: gcv_FboTexCoord is in center of sample pixels, origin is bottom left!
        // 
        // Same as flipquad - but w/ rgss coordinates

        float sampleCount = gcu_FboTexSize.z;
        vec2 psize = 1.0 / gcu_FboTexSize.xy; // pixel size

        vec2 normFragCoord = gl_FragCoord.xy - vec2(0.5, 0.5); // normalize center 0.5/0.5 -> 0/0
        vec2 modPos = mod(normFragCoord, 2.0);
        float orient = mod(modPos.x + modPos.y, 2.0); // mirrored on all odd columns, alternating each row (checker-board pattern)

        vec2 texCoord = gcv_FboTexCoord.st;
        float edge1Q = ( sampleCount / 2.0 ) - 1.0;

        vec4 t;

// #define GetSample(texUnit, texCoord, psize, cx, cy, offX, offY) texture2D(texUnit, texCoord + psize *  vec2(cx+offX, cy+offY))

        if( 0.0 == orient ) {
                                                                                               // SWIPE LEFT -> RIGHT
            t  = GetSample(gcu_FboTexUnit, texCoord, psize, -edge1Q,     0.0, -0.5,  0.5)*0.25; // upper-left  [p1]
            t += GetSample(gcu_FboTexUnit, texCoord, psize,     0.0, -edge1Q, -0.5, -0.5)*0.25; // lower-left  [p3]
            t += GetSample(gcu_FboTexUnit, texCoord, psize,     0.0,  edge1Q,  0.5,  0.5)*0.25; // upper-right [p2]
            t += GetSample(gcu_FboTexUnit, texCoord, psize,  edge1Q,     0.0,  0.5, -0.5)*0.25; // lower-right [p4]
        } else {
            t  = GetSample(gcu_FboTexUnit, texCoord, psize, -edge1Q,     0.0, -0.5, -0.5)*0.25; // lower-left  [p4]
            t += GetSample(gcu_FboTexUnit, texCoord, psize,     0.0,  edge1Q, -0.5,  0.5)*0.25; // upper-left  [p3]
            t += GetSample(gcu_FboTexUnit, texCoord, psize,     0.0, -edge1Q,  0.5, -0.5)*0.25; // lower-right [p2]
            t += GetSample(gcu_FboTexUnit, texCoord, psize,  edge1Q,     0.0,  0.5,  0.5)*0.25; // upper-right [p1]
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

