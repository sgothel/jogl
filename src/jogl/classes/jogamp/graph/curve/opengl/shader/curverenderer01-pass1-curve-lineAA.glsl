
    // if( gcv_TexCoord.x == 10.0 && gcv_TexCoord.y == 10.0 ) {
    if( gcv_TexCoord.z > 0.0 ) {
         // pass-1: AA Lines
         #if 1
             // float dist = sqrt( gcv_TexCoord.x*gcv_TexCoord.x + gcv_TexCoord.y*gcv_TexCoord.y ); // magnitude
             float dist = sqrt( gcv_TexCoord.y*gcv_TexCoord.y ); // magnitude
             // float a = 1.0 - smoothstep (gcv_TexCoord.y-gcv_TexCoord.z, gcv_TexCoord.y, dist);
             float r = gcv_TexCoord.x/3.0;
             float wa = gcv_TexCoord.x+r;
             float waHalf = wa/2.0;
             float a = 1.0 - smoothstep (waHalf-2.0*r, waHalf, dist);
             color = vec3(0, 0, 1.0); // gcu_ColorStatic.rgb;
             alpha = a;
         #else 
             color = vec3(0, 0, 1.0);
             alpha = 1.0;
         #endif 
    } else 

