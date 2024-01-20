// Copyright 2010-2024 JogAmp Community. All rights reserved.
    
#ifdef USE_FRUSTUM_CLIPPING
    if( isOutsideMvFrustum(gcv_ClipCoord) ) {
        #if USE_DISCARD
            discard; // discard freezes NV tegra2 compiler
        #else
            mgl_FragColor = vec4(0);
        #endif
    } else
#endif     
    if( gcv_CurveParam.x == 0.0 && gcv_CurveParam.y == 0.0 ) {
        // pass-1: Lines
#if defined(USE_COLOR_TEXTURE) && defined(USE_COLOR_CHANNEL)
        vec4 t = clip_coord(gcuTexture2D(gcu_ColorTexUnit, gcv_ColorTexCoord.st), vec4(1), gcv_ColorTexCoord, vec2(0), gcu_ColorTexBBox[2]);
          
        mgl_FragColor = vec4( mix( t.rgb * gcu_ColorStatic.rgb, gcv_Color.rgb, gcv_Color.a ), 
                              mix( t.a * gcu_ColorStatic.a, 1, gcv_Color.a) ); 
#elif defined(USE_COLOR_TEXTURE)
        mgl_FragColor = clip_coord(gcuTexture2D(gcu_ColorTexUnit, gcv_ColorTexCoord.st), vec4(1), gcv_ColorTexCoord, vec2(0), gcu_ColorTexBBox[2]) 
                        * gcu_ColorStatic;
#elif defined(USE_COLOR_CHANNEL)
        mgl_FragColor = gcv_Color * gcu_ColorStatic;
#else
        mgl_FragColor = gcu_ColorStatic;
#endif
    } else {
        // pass-1: curves
        vec2 rtex = vec2( abs(gcv_CurveParam.x), abs(gcv_CurveParam.y) - 0.1 );

        vec2 dtx = dFdx(rtex);
        vec2 dty = dFdy(rtex);
          
        vec2 f = vec2((dtx.y - dtx.x + 2.0*rtex.x*dtx.x), (dty.y - dty.x + 2.0*rtex.x*dty.x));
        float position = rtex.y - (rtex.x * (1.0 - rtex.x));

        float a = clamp(0.5 - ( position/length(f) ) * sign(gcv_CurveParam.y), 0.0, 1.0);
#if defined(USE_COLOR_TEXTURE) && defined(USE_COLOR_CHANNEL)
        vec4 t = clip_coord(gcuTexture2D(gcu_ColorTexUnit, gcv_ColorTexCoord.st), vec4(1), gcv_ColorTexCoord, vec2(0), gcu_ColorTexBBox[2]);
                                              
        mgl_FragColor = vec4( mix( t.rgb * gcu_ColorStatic.rgb, gcv_Color.rgb, gcv_Color.a ), 
                              a * mix( t.a * gcu_ColorStatic.a, 1, gcv_Color.a) ); 
#elif defined(USE_COLOR_TEXTURE)
        vec4 t = clip_coord(gcuTexture2D(gcu_ColorTexUnit, gcv_ColorTexCoord.st), vec4(1), gcv_ColorTexCoord, vec2(0), gcu_ColorTexBBox[2]);
        
        mgl_FragColor = vec4(t.rgb * gcu_ColorStatic.rgb, t.a * gcu_ColorStatic.a * a);
#elif defined(USE_COLOR_CHANNEL)
        mgl_FragColor = vec4(gcv_Color.rgb * gcu_ColorStatic.rgb, gcv_Color.a * gcu_ColorStatic.a * a);
#else
        mgl_FragColor = vec4(gcu_ColorStatic.rgb, gcu_ColorStatic.a * a);
#endif
    }

