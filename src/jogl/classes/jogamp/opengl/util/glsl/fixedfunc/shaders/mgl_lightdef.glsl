#ifndef mgl_lightdef_glsl
#define mgl_lightdef_glsl

struct mgl_LightModelParameters {
   vec4 ambient; 
};
struct mgl_LightSourceParameters {
   vec4 ambient; 
   vec4 diffuse; 
   vec4 specular; 
   vec4 position; 
   // vec4 halfVector; // is computed here
   vec3 spotDirection; 
   float spotExponent; 
   float spotCutoff; // (range: [0.0,90.0], 180.0)
   //float spotCosCutoff; // (range: [1.0,0.0],-1.0)
   float constantAttenuation; 
   float linearAttenuation; 
   float quadraticAttenuation; 
};
struct mgl_MaterialParameters {
   vec4 ambient;    
   vec4 diffuse;    
   vec4 specular;   
   vec4 emission;   
   float shininess; 
};

#endif // mgl_lightdef_glsl
