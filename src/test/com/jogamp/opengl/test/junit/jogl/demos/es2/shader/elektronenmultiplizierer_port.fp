/**
 * Copyright 2011 JogAmp Community. All rights reserved.
 * 
 * Details see: src/test/com/jogamp/opengl/test/junit/jogl/demos/es2/ElektronenMultiplizierer.java
 */

/**
 * http://www.youtube.com/user/DemoscenePassivist
 * author: Dominik Stroehlein (DemoscenePassivist) 
 **/

//When I wrote this, only God and I understood what I was doing ...
// ... now only God knows! X-)

uniform int en;
uniform float et;
uniform sampler2D fb;
uniform float br,tm;
uniform vec2 resolution;
float v;
vec3 n;
vec2 e;
vec3 f=vec3(0,.6,.46);
mat3 i,m,r,y;
vec2 c;
const float x=1.618,t=1./sqrt(pow(x*(1.+x),2.)+pow(x*x-1.,2.)+pow(1.+x,2.)),z=x*(1.+x)*t,s=(x*x-1.)*t,w=(1.+x)*t;
const vec3 a=vec3(.5,.5/x,.5*x),p=vec3(z,s,w);
vec3 l(vec3 v) {
    vec3 n;
    if(en==6)
        n=vec3(.61,.1*et,.99);
    else
      n=vec3(.61,0.,.99);
    float e=2.;
    v*=i;
    float f,x,c=1000.,y=0.;
    for(int z=0;z<8;z++) {
        v*=m;
        v=abs(v);
        x=v.x*a.z+v.y*a.y-v.z*a.x;
        if(x<0.)
            v+=vec3(-2.,-2.,2.)*x*a.zyx;
        x=-v.x*a.x+v.y*a.z+v.z*a.y;
        if(x<0.)
            v+=vec3(2.,-2.,-2.)*x*a.xzy;
        x=v.x*a.y-v.y*a.x+v.z*a.z;
        if(x<0.)
            v+=vec3(-2.,2.,-2.)*x*a.yxz;
        x=-v.x*p.x+v.y*p.y+v.z*p.z;
        if(x<0.)
            v+=vec3(2.,-2.,-2.)*x*p.xyz;
        x=v.x*p.z-v.y*p.x+v.z*p.y;
        if(x<0.)
            v+=vec3(-2.,2.,-2.)*x*p.zxy;
        v*=r;
        v*=e;
        v-=n*(e-1.);
        f=dot(v,v);
        if(z<4)
            c=min(c,f),y=f;
    }
    return vec3((length(v)-2.)*pow(e,-8.),c,y);
}
vec3 b(vec2 x) {
    vec2 n=(.5*e-x)/vec2(e.x,-e.y);
    n.x*=e.x/e.y;
    vec3 a=n.x*vec3(1,0,0)+n.y*vec3(0,1,0)-v*vec3(0,0,1);
    return normalize(y*a);
}
float b(vec3 e,vec3 x,float v) {
    float n=1.;
    v*=10.6;
    float t=.16/v,y=2.*v;
    for(int i=0;i<5;++i)
        n-=(y-l(e+x*y).x)*t,y+=v,t*=.5;
    return clamp(n,0.,1.);
}
vec4 h(vec2 x) {
    vec3 a=b(x);
    float i=6e-05;
    vec3 y=n+i*a;
    float c=6e-07;
    vec3 m,z=vec3(0);
    int r=0;
    bool t=false;
    float s=0.,w=25.,p=2.*(1./sqrt(1.+v*v))*(1./min(e.x,e.y))*1.22;
    i=s;
    y=n+i*a;
    for(int g=0;g<90;g++) {
        r=g;
        m=l(y);
        m.x*=.53;
        if(t&&m.x<c||i>w||i<s) {
            r--;
            break;
        }
        t=false;
        i+=m.x;
        y=n+i*a;
        c=i*p;
        if(m.x<c||i<s)
            t=true;
    }
    vec4 g=vec4(f,.5);
    if(t) {
        float d=1.;
        if(r<1||i<s)
            z=normalize(y);
        else {
            float h=max(c*.5,1.5e-07);
            z=normalize(vec3(l(y+vec3(h,0,0)).x-l(y-vec3(h,0,0)).x,l(y+vec3(0,h,0)).x-l(y-vec3(0,h,0)).x,l(y+vec3(0,0,h)).x-l(y-vec3(0,0,h)).x));
            d=b(y,z,c);
        }
        float h=max(dot(z,normalize(vec3(-66,162,-30)-y)),0.);
        g.xyz=(mix(vec3(.5),f,.3)*vec3(.45)+vec3(.45)*h+pow(h,4.)*.8)*d;
        g.w=1.;
    }
    g.xyz=mix(f,g.xyz,exp(-pow(i,2.)*.01));
    return g;
}
mat3 g(float e) {
    return mat3(vec3(1.,0.,0.),vec3(0.,cos(e),sin(e)),vec3(0.,-sin(e),cos(e)));
}
mat3 d(float e) {
    return mat3(vec3(cos(e),0.,-sin(e)),vec3(0.,1.,0.),vec3(sin(e),0.,cos(e)));
}
vec4 D(vec2 x) {
    mat3 a=mat3(1,0,0,0,1,0,0,0,1);
    float t=sin(.1*tm),z=cos(.1*tm);
    mat3 p=mat3(vec3(z,t,0.),vec3(-t,z,0.),vec3(0.,0.,1.));
    vec2 f;
    float s,w;
    f=c.xy;
    v=1.;
    if(en==2)
        e=vec2(384,384),n=vec3(0.,0.,-2.7);
    if(en==3)
        n=vec3(0.,0.,-2.7*(10.-et));
    if(en==4)
        n=vec3(0.,0.,-1.89),s=et,w=0.;
    if(en==5)
        n=vec3(0.,0.,-.05),s=1.06,w=-1.-et;
    if(en==6)
        n=vec3(0.,0.,-1.35),s=et,w=sin(et*.03)-1.;
    if(en==7)
        e=vec2(384,384),s=et,w=sin(et*.93)-1.,n=vec3(0.,0.,-2.7);
    i=g(.1*tm)*d(.1*tm)*p*a;
    m=g(s)*a;
    r=g(w)*a;
    y=d(3.14)*a;
    vec4 l=h(f);
    if(l.w<.00392) {
        discard;
    }
    return l;
}
vec4 D(vec4 e,vec2 x) {
    vec2 n=vec2(.24,-.24);
    float y;
    if(en==0)
        y=.625;
    else
     y=.325;
    vec2 v=.5+(x/y-n);
    vec4 c=texture2D(fb,v);
    if(c.w>0.)
        e=mix(e,c,c.w);
    return e;
}
vec4 o(vec2 v) {
    float n=2.;
    vec3 e=vec3(1.);
    vec4 i=vec4(e,0.);
    float y=0.;
    vec2 x;
    if(en==0)
        x=vec2(sin(et+2.07)*.05,cos(et+2.07));
    else
        x=v;
    for(int f=0;f<128;f++) {
        y+=1.;
        float t=pow(length(v),n),c=n*atan(v.y,v.x);
        v=vec2(cos(c)*t,sin(c)*t)+x;
        if(y>=1.) {
            i=D(i,v);
            if(i.w>=.6) {
                break;
            }
        }
    }
    float c=clamp(1.-y/128.*2.,0.,1.);
    i.xyz=mix(e,i.xyz,c);
    return i;
}
void main() {
    //vec2 v=vec2(640.0,480.0);
    vec2 v =resolution;
    e=v;
    c=gl_FragCoord.xy;
    vec4 n;
    if(en==0||en==1) {
        vec3 a;
        if(en==0)
            a=vec3(-.2,-.515,.095347+et*1.75);
        else
            a=vec3(.325895,.049551,.0005+et);
        vec2 x=(c.xy-v*.5)/v*vec2(v.x/v.y,1.)*a.z+a.xy;
        n=o(x);
    } else
        n=D(c.xy);
    if(en==2||en==7)
        gl_FragColor=n;
    else {
        vec2 i=c.xy/v.xy;
        i.y*=-1.;
        vec3 x=n.xyz;
        x=clamp(x*.5+.5*x*x*1.2,0.,1.);
        x*=.5+8.*i.x*i.y*(1.-i.x)*(-1.-i.y);
        if(en==0||en==3)
            x*=vec3(.8,1.,.7);
        if(en==1||en==4)
            x*=vec3(.95,.85,1.);
        if(en==5)
            x*=vec3(1.,.7,1.);
        if(en==6)
            x*=vec3(.7,1.,1.);
        if(en==2)
            x*=vec3(1.,1.,.7);
        x*=.9+.1*sin(1.5*tm+i.y*1000.);
        x*=.97+.13*sin(2.5*tm);
        x*=br;
        gl_FragColor=vec4(x,1.);
    }
}
