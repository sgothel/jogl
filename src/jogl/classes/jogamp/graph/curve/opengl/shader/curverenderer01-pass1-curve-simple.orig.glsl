
    vec2 rtex = vec2( abs(gcv_TexCoord.x), abs(gcv_TexCoord.y) );

    if( gcv_TexCoord.x == 0.0 && gcv_TexCoord.y == 0.0 ) {
        // pass-1: Lines
        color = gcu_ColorStatic.rgb;
        alpha = 1.0;
    } else if ( gcv_TexCoord.x > 0.0 && ( rtex.y > 0.0 || rtex.x == 1.0 ) ) {
        // pass-1: curves
        rtex.y -= 0.1;

        if(rtex.y < 0.0 && gcv_TexCoord.y < 0.0) {
            // discard; // freezes NV tegra2 compiler
            color = zero3;
            alpha = 0.0;
        } else {
            rtex.y = max(rtex.y, 0.0); // always >= 0 

            vec2 dtx = dFdx(rtex);
            vec2 dty = dFdy(rtex);
              
            vec2 f = vec2((dtx.y - dtx.x + 2.0*rtex.x*dtx.x), (dty.y - dty.x + 2.0*rtex.x*dty.x));
            float position = rtex.y - (rtex.x * (1.0 - rtex.x));

            color = gcu_ColorStatic.rgb;
            alpha = clamp(0.5 - ( position/length(f) ) * sign(gcv_TexCoord.y), 0.0, 1.0);
        }
    } else {
        color = zero3;
        alpha = 0.0;
    }

