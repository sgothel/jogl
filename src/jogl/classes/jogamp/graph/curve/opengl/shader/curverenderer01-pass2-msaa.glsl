        // Pass-2: Dump Texture
        vec4 t = texture2D(gcu_FboTexUnit, gcv_FboTexCoord.st);
        #if 0
        if( 0.0 == t.a ) {
          discard; // discard freezes NV tegra2 compiler
        }
        #endif

        mgl_FragColor = t;

