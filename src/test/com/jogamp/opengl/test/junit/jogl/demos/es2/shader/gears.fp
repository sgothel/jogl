// Copyright (C) 2011 JogAmp Community. All rights reserved.
// Details see GearsES2.java

#ifdef GL_ES
  precision mediump float;
  precision mediump int;
#endif

uniform vec4 color;

varying vec3 normal;
varying vec4 position;
varying vec3 lightDir;
varying float attenuation;
varying vec3 cameraDir;

// Defining The Material Colors
const vec4 matAmbient  = vec4(0.2, 0.2, 0.2, 1.0); // orig default
const vec4 matDiffuse  = vec4(0.8, 0.8, 0.8, 1.0); // orig default
// const vec4 matSpecular = vec4(0.0, 0.0, 0.0, 1.0); // orig default
const vec4 matSpecular = vec4(0.8, 0.8, 0.8, 1.0);
// const float matShininess = 0.0; // orig default
const float matShininess = 0.5;

void main()
{  
    vec4 ambient = color * matAmbient;
    vec4 specular = vec4(0.0);
    
    float lambertTerm = dot(normal, lightDir);       
    vec4 diffuse = color * lambertTerm *  attenuation * matDiffuse;
    if (lambertTerm > 0.0) {
        float NdotHV;
        /*
        vec3 halfDir = normalize (lightDir + cameraDir); 
        NdotHV   = max(0.0, dot(normal, halfDir));
        */      
        vec3 E = normalize(-position.xyz);  
        vec3 R = reflect(-lightDir, normal);
        NdotHV   = max(0.0, dot(R, E));
        
        specular += color * pow(NdotHV, matShininess) * attenuation * matSpecular;
    }
        
    gl_FragColor = ambient + diffuse + specular ;
}
