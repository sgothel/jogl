
    vec2 rtex = vec2( abs(gcv_CurveParam.x), abs(gcv_CurveParam.y) );

    if( gcv_CurveParam.x == 0.0 && gcv_CurveParam.y == 0.0 ) {
        // pass-1: Lines
#if defined(USE_COLOR_TEXTURE) && defined(USE_COLOR_CHANNEL)
        mgl_FragColor = gcuTexture2D(gcu_ColorTexUnit, gcv_ColorTexCoord.st) * gcv_Color * gcu_ColorStatic;
#elif defined(USE_COLOR_TEXTURE)
        mgl_FragColor = gcuTexture2D(gcu_ColorTexUnit, gcv_ColorTexCoord.st) * gcu_ColorStatic;
#elif defined(USE_COLOR_CHANNEL)
        mgl_FragColor = gcv_Color * gcu_ColorStatic;
#else
        mgl_FragColor = gcu_ColorStatic;
#endif
    } else if ( gcv_CurveParam.x > 0.0 && ( rtex.y > 0.0 || rtex.x == 1.0 ) ) {
        // pass-1: curves
        rtex.y -= 0.1;

        if(rtex.y < 0.0 && gcv_CurveParam.y < 0.0) {
            #if USE_DISCARD
                discard; // freezes NV tegra2 compiler
            #else
                mgl_FragColor = vec4(0);
            #endif
        } else {
            rtex.y = max(rtex.y, 0.0); // always >= 0 

            vec2 dtx = dFdx(rtex);
            vec2 dty = dFdy(rtex);
              
            vec2 f = vec2((dtx.y - dtx.x + 2.0*rtex.x*dtx.x), (dty.y - dty.x + 2.0*rtex.x*dty.x));
            float position = rtex.y - (rtex.x * (1.0 - rtex.x));

            float a = clamp(0.5 - ( position/length(f) ) * sign(gcv_CurveParam.y), 0.0, 1.0);
#if defined(USE_COLOR_TEXTURE) && defined(USE_COLOR_CHANNEL)
            vec4 t = gcuTexture2D(gcu_ColorTexUnit, gcv_ColorTexCoord.st);
            mgl_FragColor = vec4(t.rgb * gcv_Color.rgb * gcu_ColorStatic.rgb, t.a * gcv_Color.a * gcu_ColorStatic.a * a);
#elif defined(USE_COLOR_TEXTURE)
            vec4 t = gcuTexture2D(gcu_ColorTexUnit, gcv_ColorTexCoord.st);
            mgl_FragColor = vec4(t.rgb * gcu_ColorStatic.rgb, t.a * gcu_ColorStatic.a * a);
#elif defined(USE_COLOR_CHANNEL)
            mgl_FragColor = vec4(gcv_Color.rgb * gcu_ColorStatic.rgb, gcv_Color.a * gcu_ColorStatic.a * a);
#else
            mgl_FragColor = vec4(gcu_ColorStatic.rgb, gcu_ColorStatic.a * a);
#endif
        }
    } else {
        mgl_FragColor = vec4(0);
    }

