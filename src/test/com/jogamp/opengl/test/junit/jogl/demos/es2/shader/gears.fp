// Copyright (C) 2011 JogAmp Community. All rights reserved.
// Details see GearsES2.java

#ifdef GL_ES
  #define MEDIUMP mediump
  #define HIGHP highp
#else
  #define MEDIUMP
  #define HIGHP
#endif

uniform MEDIUMP vec4 color;

varying MEDIUMP vec3 normal;
varying MEDIUMP vec4 position;
varying MEDIUMP vec3 lightDir;
varying MEDIUMP float attenuation;
varying MEDIUMP vec3 cameraDir;

// Defining The Material Colors
const MEDIUMP vec4 matAmbient  = vec4(0.2, 0.2, 0.2, 1.0); // orig default
const MEDIUMP vec4 matDiffuse  = vec4(0.8, 0.8, 0.8, 1.0); // orig default
// const MEDIUMP vec4 matSpecular = vec4(0.0, 0.0, 0.0, 1.0); // orig default
const MEDIUMP vec4 matSpecular = vec4(0.8, 0.8, 0.8, 1.0);
// const MEDIUMP float matShininess = 0.0; // orig default
const MEDIUMP float matShininess = 0.5;

void main()
{ 
    MEDIUMP float lambertTerm = dot(normal, lightDir);       
 
    MEDIUMP vec4 ambient = color * matAmbient;
    MEDIUMP vec4 diffuse = color * lambertTerm *  attenuation * matDiffuse;
    MEDIUMP vec4 specular = vec4(0.0);
    if (lambertTerm > 0.0) {
        float NdotHV;
        /*
        MEDIUMP vec3 halfDir;        
        halfDir  = normalize (lightDir + cameraDir); 
        NdotHV   = max(0.0, dot(normal, halfDir));
        */      
        vec3 E = normalize(-position.xyz);  
        vec3 R = reflect(-lightDir, normal);
        NdotHV   = max(0.0, dot(R, E));
        
        specular += color * pow(NdotHV, matShininess) * attenuation * matSpecular;
    }
        
    gl_FragColor = ambient + diffuse + specular ;
}
