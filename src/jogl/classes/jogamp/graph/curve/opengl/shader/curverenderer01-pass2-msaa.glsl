        // Pass-2: Dump Texture
        vec4 t = texture2D(gcu_FboTexUnit, gcv_FboTexCoord.st);
        #if USE_DISCARD
            if( 0.0 == t.a ) {
                discard; // discard freezes NV tegra2 compiler
            } else {
                mgl_FragColor = t;
            }
        #else
            mgl_FragColor = t;
        #endif

