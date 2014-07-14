
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
          
        float w = gcu_Weight;
        float pd = ((2.0 - (2.0*w))*rtex.x*rtex.x) + 2.0*(w-1.0)*rtex.x + 1.0;
        float position = rtex.y - ((w*rtex.x*(1.0 - rtex.x))/pd);

        float aph = 2.0 - 2.0*w;
        
        float gd = (aph*rtex.x*rtex.x + 2.0*rtex.x + 1.0)*(aph*rtex.x*rtex.x + 2.0*rtex.x + 1.0);
        vec2 f = vec2((dtx.y - (w*dtx.x*(1.0 - 2.0*rtex.x))/gd), (dty.y - (w*dty.x*(1.0 - 2.0*rtex.x))/gd));

        float a = clamp(0.5 - ( position/length(f) ) * sign(gcv_CurveParam.y), 0.0, 1.0);
#ifdef USE_COLOR_CHANNEL
        mgl_FragColor = vec4(gcv_Color.rgb * gcu_ColorStatic.rgb, gcv_Color.a * gcu_ColorStatic.a * a);
#else
        mgl_FragColor = vec4(gcu_ColorStatic.rgb, gcu_ColorStatic.a * a);
#endif
    }

