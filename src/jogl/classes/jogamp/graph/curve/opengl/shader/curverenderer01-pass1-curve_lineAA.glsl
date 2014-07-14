
    // if( gcv_CurveParam.x == 10.0 && gcv_CurveParam.y == 10.0 ) {
    if( gcv_CurveParam.z > 0.0 ) {
         // pass-1: AA Lines
         #if 1
             // float dist = sqrt( gcv_CurveParam.x*gcv_CurveParam.x + gcv_CurveParam.y*gcv_CurveParam.y ); // magnitude
             float dist = sqrt( gcv_CurveParam.y*gcv_CurveParam.y ); // magnitude
             // float a = 1.0 - smoothstep (gcv_CurveParam.y-gcv_CurveParam.z, gcv_CurveParam.y, dist);
             float r = gcv_CurveParam.x/3.0;
             float wa = gcv_CurveParam.x+r;
             float waHalf = wa/2.0;
             float a = 1.0 - smoothstep (waHalf-2.0*r, waHalf, dist);
             // mgl_FragColor = vec4(gcu_ColorStatic.rgb, gcu_ColorStatic.a * a);
             mgl_FragColor = vec4(0, 0, 1.0, gcu_ColorStatic.a * a);
         #else 
             mgl_FragColor = vec4(0, 0, 1.0, 1.0);
         #endif 
    } else 

