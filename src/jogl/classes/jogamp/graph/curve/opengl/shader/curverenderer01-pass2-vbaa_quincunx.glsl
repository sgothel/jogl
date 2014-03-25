
        // Pass-2: AA on Texture
        // Note: gcv_TexCoord is in center of sample pixels, origin is bottom left!

        const float sampleCount = gcu_TextureSize.z;
        const vec2 psize = 1.0 / gcu_TextureSize.xy; // pixel size

        // Just poles (NW, SW, ..)
        const float edgeH = sampleCount / 2.0;

        const vec2 texCoord = gcv_TexCoord.st;

        vec4 t = vec4(0);

        t += GetSample(gcu_TextureUnit, texCoord, psize,    0.0,    0.0, 0.0, 0.0)*0.5;   // w1 - center
        t += GetSample(gcu_TextureUnit, texCoord, psize, -edgeH, -edgeH, 0.0, 0.0)*0.125; // w2 - sharing
        t += GetSample(gcu_TextureUnit, texCoord, psize, -edgeH,  edgeH, 0.0, 0.0)*0.125; // w3 - edges
        t += GetSample(gcu_TextureUnit, texCoord, psize,  edgeH, -edgeH, 0.0, 0.0)*0.125; // w4 - w/ all pixels
        t += GetSample(gcu_TextureUnit, texCoord, psize,  edgeH,  edgeH, 0.0, 0.0)*0.125; // w5

        #if 0
        if(t.w == 0.0){
            discard; // discard freezes NV tegra2 compiler
        }
        #endif
        
        color = t.rgb;
        alpha = gcu_Alpha * t.a;

