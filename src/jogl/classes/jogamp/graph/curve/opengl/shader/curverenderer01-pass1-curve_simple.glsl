
    if( gcv_CurveParam.x == 0.0 && gcv_CurveParam.y == 0.0 ) {
        // pass-1: Lines
#ifdef USE_COLOR_CHANNEL
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
#ifdef USE_COLOR_CHANNEL
        mgl_FragColor = vec4(gcv_Color.rgb * gcu_ColorStatic.rgb, gcv_Color.a * gcu_ColorStatic.a * a);
#else
        mgl_FragColor = vec4(gcu_ColorStatic.rgb, gcu_ColorStatic.a * a);
#endif
    }

