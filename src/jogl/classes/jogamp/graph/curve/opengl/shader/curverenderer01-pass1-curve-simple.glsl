
    if( gcv_TexCoord.x == 0.0 && gcv_TexCoord.y == 0.0 ) {
        // pass-1: Lines
        // color = vec3(0, 1.0, 0);
        color = gcu_ColorStatic.rgb;
        alpha = 1.0;
    } else {
        // pass-1: curves
        const vec2 rtex = vec2( abs(gcv_TexCoord.x), abs(gcv_TexCoord.y) - 0.1 );

        const vec2 dtx = dFdx(rtex);
        const vec2 dty = dFdy(rtex);
          
        const vec2 f = vec2((dtx.y - dtx.x + 2.0*rtex.x*dtx.x), (dty.y - dty.x + 2.0*rtex.x*dty.x));
        const float position = rtex.y - (rtex.x * (1.0 - rtex.x));

        // color = vec3(1.0, 0, 0);
        color = gcu_ColorStatic.rgb;
        alpha = clamp(0.5 - ( position/length(f) ) * sign(gcv_TexCoord.y), 0.0, 1.0);
    }

