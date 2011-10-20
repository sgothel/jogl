/**
 * Copyright 2011 JogAmp Community. All rights reserved.
 * 
 * Details see: src/test/com/jogamp/opengl/test/junit/jogl/demos/es2/ElektronenMultiplizierer.java
 */

/**
 * http://www.youtube.com/user/DemoscenePassivist
 * author: Dominik Stroehlein (DemoscenePassivist) 
 **/

#ifdef GL_ES
  precision mediump float;
  precision mediump sampler2D;
  precision mediump int;
#endif

uniform int en;         //effectnumber
uniform float et;       //effecttime
uniform sampler2D fb;   //fbotexture
uniform float br;       //brightness
uniform float tm;       //time
uniform vec2 resolution;//screen resolution/fbo resolution
 
float camerafocallengthdode;
vec3 camerapositiondode;
vec2 sizedode;
vec3 backgroundcolor = vec3(0,0.6,0.46);
mat3 worldrotationxyz;
mat3 fractalplanerotationx;
mat3 fractalplanerotationy;
mat3 camerarotationdode;
vec2 oglFragCoord;

//fractal formula used for sphreretracing/distance-estimation
//dodecahedron serpinski (knighty)
//http://www.fractalforums.com/index.php?topic=3158.msg16982#msg16982
//normal vectors for the dodecahedra-siepinski folding planes are:
//(phi^2, 1, -phi), (-phi, phi^2, 1), (1, -phi, phi^2), (-phi*(1+phi), phi^2-1, 1+phi), (1+phi, -phi*(1+phi), phi^2-1) and x=0, y=0, z=0 planes.

//const pre-calc
const float phi = 1.618;
// Using math functions in initializers fails on MacOSX
// const float _IKVNORM_ = 1.0 / sqrt(pow(phi * (1.0 + phi), 2.0) + pow(phi * phi - 1.0, 2.0) + pow(1.0 + phi, 2.0));
const float _IKVNORM_ = 0.190989113930771;
const float _C1_ = phi * (1.0 + phi) * _IKVNORM_;
const float _C2_ = (phi * phi - 1.0) * _IKVNORM_;
const float _1C_ = (1.0 + phi) * _IKVNORM_;
const vec3 phi3 = vec3(0.5, 0.5 / phi, 0.5 * phi);
const vec3 c3   = vec3(_C1_, _C2_, _1C_);

vec3 distancefunction(vec3 w) {
//!P center scale offset ...   
    vec3 offset;
    if (en==6) {
        offset = vec3(0.61,0.1*et,0.99);
    } else {
        offset = vec3(0.61,0.0,0.99);
    } 
//!P center scale \0/ this is awesome for fadeins !!!
    float scale = 2.;
    w *= worldrotationxyz;
    float d, t;
    float md = 1000.0, cd = 0.0;
//!P iterations (8) ... 2x see below
    for (int i = 0; i < 8; i++) {
        w *= fractalplanerotationx;
        w = abs(w);        
        t = w.x * phi3.z + w.y * phi3.y - w.z * phi3.x;
        if (t < 0.0) { w += vec3(-2.0, -2.0, 2.0) * t * phi3.zyx; }
        t = -w.x * phi3.x + w.y * phi3.z + w.z * phi3.y;
        if (t < 0.0) { w += vec3(2.0, -2.0, -2.0) * t * phi3.xzy; }
        t = w.x * phi3.y - w.y * phi3.x + w.z * phi3.z;
        if (t < 0.0) { w += vec3(-2.0, 2.0, -2.0) * t * phi3.yxz; } 
        t = -w.x * c3.x + w.y * c3.y + w.z * c3.z;
        if (t < 0.0) { w += vec3(2.0, -2.0, -2.0) * t * c3.xyz; }
        t = w.x * c3.z - w.y * c3.x + w.z * c3.y;
        if (t < 0.0) { w += vec3(-2.0, 2.0, -2.0) * t * c3.zxy; }     
        w *= fractalplanerotationy;
        w *= scale;
        w -= offset * (scale - 1.0);
        //accumulate minimum orbit for coloring ... 
        d = dot(w, w);
//!P iterations for coloring (4)        
        if (i < 4) {
            md = min(md, d);
            cd = d;
        }
    }
//!P max iterations (8)        
    return vec3((length(w) - 2.0) * pow(scale, -8.0), md, cd);
}

//calculate ray direction fragment coordinates
vec3 raydirection(vec2 pixel) {
    vec2 p = (0.5*sizedode-pixel)/vec2(sizedode.x,-sizedode.y);
//!P aspect ratio of dode
    p.x *= sizedode.x/sizedode.y;
//!P vec3 w = vec3(0, 0, 1), vec3 v = vec3(0, 1, 0), vec3 u = vec3(1, 0, 0);   
    vec3 d = (p.x * vec3(1, 0, 0)+p.y * vec3(0, 1, 0)-camerafocallengthdode * vec3(0, 0, 1));   
    return normalize(camerarotationdode * d);
}

//iq's fake ambient occlusion
//http://www.iquilezles.org/www/material/nvscene2008/rwwtt.pdf
//http://www.iquilezles.org/www/articles/ao/ao.htm
float ambientocclusion(vec3 p, vec3 n, float eps) {
    float o = 1.0;
//!P ao spread (10.6)
//   spreads the output color intensity 
    eps *= 10.6;
//!P ao intensity (0.16)
    float k = 0.16 / eps;
    //add little start distance to the surface
    float d = 2.0 * eps;
//!P ao iterations (5) ...    
    for (int i = 0; i < 5; ++i) {
        o -= (d - distancefunction(p + n * d).x) * k;
        d += eps;
        //fade ao when distance to the surface increases
        k *= 0.5;
    }
    return clamp(o, 0.0, 1.0);
}

vec4 render(vec2 pixel) {
    vec3  ray_direction = raydirection(pixel);
//!P minimum ray length (6e-5)    
    float ray_length = 6e-5;
    vec3  ray = camerapositiondode + ray_length * ray_direction;
//!P minimum epsilon (6e-7) ...
    float eps = 6e-7;
    vec3  dist;
    vec3  normal = vec3(0);
    int   steps = 0;
    bool  hit = false;
    float minmarch = 0.0;
//!P maxmarch = 10000.0;
    float maxmarch = 25.0;
//!P field of view scale = (1.0 / sqrt(1.0 + camerafocallengthdode * camerafocallengthdode))   
//!P detail of surface approximation =  1.22
//!P pixelscale = (1.0 / min(sizedode.x, sizedode.y))
    float epsfactor = 2.0 * (1.0 / sqrt(1.0 + camerafocallengthdode * camerafocallengthdode)) * (1.0 / min(sizedode.x, sizedode.y)) * 1.22;    
    ray_length = minmarch;
    ray = camerapositiondode + ray_length * ray_direction;
//!P max number of raymarching steps (90);
    for (int i = 0; i < 90; i++) {
        steps = i;
        dist = distancefunction(ray);
//!P X-) questionable surface smoothing (0.53)            
        dist.x *= 0.53;            
        //backtrack previous step and check if it was only a "fussel"
        if (hit && dist.x < eps || ray_length > maxmarch || ray_length < minmarch) {
            steps--;
            break;
         }
         hit = false;
         ray_length += dist.x;
         ray = camerapositiondode + ray_length * ray_direction;
         eps = ray_length * epsfactor;
         if (dist.x < eps || ray_length < minmarch) {
             hit = true;
         }
    }
    //\0/ there is a hit!
    vec4 color = vec4(backgroundcolor,0.5);
    if (hit) {
        float aof = 1.0;
        if (steps < 1 || ray_length < minmarch) {
            normal = normalize(ray);
        } else {
            //gradient in x,y and z direction for intersection point 
            //!P minimum normal (1.5e-7)
            float e = max(eps * 0.5, 1.5e-7);
            normal = normalize(vec3(
                distancefunction(ray + vec3(e, 0, 0)).x - distancefunction(ray - vec3(e, 0, 0)).x, 
                distancefunction(ray + vec3(0, e, 0)).x - distancefunction(ray - vec3(0, e, 0)).x, 
                distancefunction(ray + vec3(0, 0, e)).x - distancefunction(ray - vec3(0, 0, e)).x
            ));
            aof = ambientocclusion(ray, normal, eps);
        }        
//!P hardcoded light position vec3(-50,150,-25)
        float diffuse = max(dot(normal, normalize(vec3(-50,150,-25) - ray)), 0.0);
//blinn/phong specular stuff ...
//!P specular exponent (4)
//!P specularity (0.8)
//!P diffuse color vec3(0.45) 2x in one line ...
//!P ambient color vec2 ambientcolor = vec2(0.5,0.3)
        color.rgb = (mix(vec3(0.5), backgroundcolor, 0.3) * vec3(0.45) + vec3(0.45) * diffuse + pow(diffuse, 4.) * 0.8)*aof;
        color.a = 1.0;
    }  
//!P fog factor = 0.01   
    color.rgb = mix(backgroundcolor, color.rgb, exp(-pow(ray_length, 2.0) * 0.01));
    return color;
}

mat3 xmatrixrotation(float angle) {
    return mat3(
        vec3(1.0,         0.0,        0.0),
        vec3(0.0,  cos(angle), sin(angle)),
        vec3(0.0, -sin(angle), cos(angle))
    );
}

mat3 ymatrixrotation(float angle) {
    return mat3(
        vec3(cos(angle), 0.0, -sin(angle)),
        vec3(       0.0, 1.0,         0.0),
        vec3(sin(angle), 0.0,  cos(angle))
    );
}

vec4 raymarch_orbittrap_image(vec2 fragcoord) {    
    //do the matrix calculations by hand X-)
    //as mat4 constructor and arithmetic assignments are 
    //currently broken (2010-09-21) on ATI cards i found
    //a workaround using vec4 constructors wich works on
    //both NVIDIA+ATI --- MAGIC. DO NOT TOUCH! -=#:-)     
    mat3  identitymatrix = mat3(1,0,0,0,1,0,0,0,1);    
    float sin_phi = sin(0.1*tm);
    float cos_phi = cos(0.1*tm);
    mat3 zrot = mat3(
        vec3( cos_phi, sin_phi, 0.0),
        vec3(-sin_phi, cos_phi, 0.0),
        vec3(     0.0,     0.0, 1.0)
    );
    vec2 position;
    float fractalplanex_var;
    float fractalplaney_var;
    position = oglFragCoord.xy;
    camerafocallengthdode = 1.0;      
    if (en==2) {
        sizedode = vec2(384,384);        
        camerapositiondode = vec3(0.0,0.0,-2.7);
    } else if (en==3) {
        camerapositiondode = vec3(0.0,0.0,-2.7*(10.-et));
    } else if (en==4) {
        camerapositiondode = vec3(0.0,0.0,-2.7*(10.-9.3));
        fractalplanex_var = et;
        fractalplaney_var = 0.0;
    } else if (en==5) {
        //inside effect
        camerapositiondode = vec3(0.0,0.0,-0.05);
        fractalplanex_var = 1.06;
        fractalplaney_var = -1.0-et;          
    } else if (en==6) {
        camerapositiondode = vec3(0.0,0.0,-2.7*(10.-9.5));
        fractalplanex_var = et;
        fractalplaney_var = sin(et*0.03)-1.0;   
    } else if (en==7) {
        sizedode = vec2(384,384);        
        fractalplanex_var = et;
        fractalplaney_var = sin(et*0.93)-1.0;
        camerapositiondode = vec3(0.0,0.0,-2.7);
    } 
    worldrotationxyz = xmatrixrotation(0.1*tm)*ymatrixrotation(0.1*tm)*zrot*identitymatrix;
    fractalplanerotationx = xmatrixrotation(fractalplanex_var)*identitymatrix;
    fractalplanerotationy = xmatrixrotation(fractalplaney_var)*identitymatrix;
    camerarotationdode = ymatrixrotation(3.14)*identitymatrix;                             
    vec4 color = render(position);
    return color;
}

//----------------------------------------------------------------------------------------------------------

vec4 orbitmapping(vec4 c, vec2 w) {
//!P orbit trap scale and offset    
    vec2 orbittrapoffset = vec2(0.24,-0.24);
    float orbittrapscale;
    if (en==0) {
        //julia set ...
        orbittrapscale = 0.625;
    } else {
        //mandlebrot ...
        orbittrapscale = 0.325;
    }
    vec2 sp = 0.5 + (w / orbittrapscale - orbittrapoffset);    
    vec4 s = texture2D(fb, sp);
    if (s.a > 0.0) {
        c = mix(c, s, s.a);
    }
    return c;
}

vec4 orbittrap(vec2 z) {
    float powerjulia = 2.;
    vec3  colorjulia = vec3(1.0);
    vec4  color = vec4(colorjulia, 0.0);
    float n = 0.0;
    vec2 c;
    if (en==0) {    
        //julia mode ...
//!P use offset-julia from 2.25 to 2.5
        c = vec2(sin(et+2.07)*0.05,cos(et+2.07));
    } else {
        //mandelbrot mode ...
        c = z;
    }
//!P max iterations for julia (128) ... 2x parameter - see below!     
    for (int i = 0; i<128; i++) {
        n += 1.0;        
        float r = pow(length(z), powerjulia);
        float a = powerjulia * atan(z.y, z.x);
        z = vec2(cos(a) * r, sin(a) * r) +c;
//!P min iterations for julia (1.0) ...         
        if (n >= 1.0) {
            color = orbitmapping(color, z);
//!P orbit trap alpha precision (0.6) ...
            if (color.a >= 0.6) {
                break;
            }
        }
    }
//!P max iterations for julia (128.0) ...
    float blend = clamp(1.0 - (n / 128.0) * 2.0, 0.0, 1.0);
    color.rgb = mix(colorjulia, color.rgb, blend);
    return color;
}

void main() {
    vec2 sizejulia = resolution;
    sizedode = sizejulia;
    oglFragCoord = gl_FragCoord.xy;
    vec4 color;
    if (en==0 || en==1) {
        //render 2d julia/mandelbrot
//!P camera position for julia ...
        vec3 camerapositionjulia;
        if (en==0) {
            //julia
            camerapositionjulia = vec3(-0.2,-0.515,0.095347+(et*1.75));
        } else {
            //mandelbrot
            camerapositionjulia = vec3(0.325895,0.049551,0.0005+et);
        }
//!P absolute output size of julia orbit trap ...
        vec2  z = ((oglFragCoord.xy - (sizejulia * 0.5)) / sizejulia) *
                    vec2(sizejulia.x/sizejulia.y, 1.0) * //aspect ratio 
                         camerapositionjulia.z + 
                         camerapositionjulia.xy;
        color = orbittrap(z);
    } else {
        color = raymarch_orbittrap_image(oglFragCoord.xy);
    } 
    if (en==2 || en==7) {
        gl_FragColor = color;
    } else {
        //do normal rendering ...    
        //analog-tv distortion ...
        vec2 position = oglFragCoord.xy / sizejulia.xy;
        position.y *=-1.0;
        vec3 color_tv = color.rgb;
        //contrast
        color_tv = clamp(color_tv*0.5+0.5*color_tv*color_tv*1.2,0.0,1.0);
        //circular vignette fade
        color_tv *= 0.5 + 0.5*16.0*position.x*position.y*(1.0-position.x)*(-1.0-position.y);                
        //color shift
        if (en==0 || en==3) {
            color_tv *= vec3(0.8,1.0,0.7); //green
        }
        if (en==1 || en==4) {
            color_tv *= vec3(0.95,0.85,1.0); //blue
        }
        if (en==5) {
            color_tv *= vec3(1.0,0.7,1.0); //purple
        }
        
        if (en==6) {
            color_tv *= vec3(0.7,1.0,1.0); //cyan
        }        
        if (en==2) {
            color_tv *= vec3(1.0,1.0,0.7); //yellow
        }
        //tvlines effect
        color_tv *= 0.9+0.1*sin(1.5*tm+position.y*1000.0);
        //tv flicker effect
        color_tv *= 0.97+0.13*sin(2.5*tm);
        color_tv *= br;
        gl_FragColor = vec4(color_tv,1.0);
    }       
}
