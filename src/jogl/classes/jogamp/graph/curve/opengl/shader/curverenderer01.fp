//Copyright 2010 JogAmp Community. All rights reserved.
//#version 100

uniform float p1y;
uniform float g_alpha;
uniform vec3 g_color;
uniform float a_strength;

varying vec2 v_texCoord;

vec3 b_color = vec3(0.0, 0.0, 0.0);

uniform sampler2D texture;
vec4 weights = vec4(0.075, 0.06, 0.045, 0.025);

void main (void)
{
	vec2 rtex = vec2(abs(v_texCoord.x),abs(v_texCoord.y));
	vec3 c = g_color;
	
	float alpha = 0.0;
	
	if((v_texCoord.x == 0.0) && (v_texCoord.y == 0.0)){
	 	alpha = g_alpha;
	}
	else if((v_texCoord.x >= 5.0)){
		vec2 dfx = dFdx(v_texCoord);
		vec2 dfy = dFdy(v_texCoord);
		
		vec2 size = 1.0/textureSize(texture,0); //version 130
		rtex -= 5.0;
		vec4 t = texture2D(texture, rtex)* 0.18;

		t += texture2D(texture, rtex + size*(vec2(1, 0)))*weights.x;
		t += texture2D(texture, rtex - size*(vec2(1, 0)))*weights.x;
		t += texture2D(texture, rtex + size*(vec2(0, 1)))*weights.x;
		t += texture2D(texture, rtex - size*(vec2(0, 1)))*weights.x;
		
		t += texture2D(texture, rtex + 2.0*size*(vec2(1, 0))) *weights.y;
		t += texture2D(texture, rtex - 2.0*size*(vec2(1, 0)))*weights.y;
		t += texture2D(texture, rtex + 2.0*size*(vec2(0, 1)))*weights.y; 
		t += texture2D(texture, rtex - 2.0*size*(vec2(0, 1)))*weights.y;
		
		t += texture2D(texture, rtex + 3.0*size*(vec2(1, 0))) *weights.z;
		t += texture2D(texture, rtex - 3.0*size*(vec2(1, 0)))*weights.z;
		t += texture2D(texture, rtex + 3.0*size*(vec2(0, 1)))*weights.z;
		t += texture2D(texture, rtex - 3.0*size*(vec2(0, 1)))*weights.z;
		
		t += texture2D(texture, rtex + 4.0*size*(vec2(1, 0))) *weights.w;
		t += texture2D(texture, rtex - 4.0*size*(vec2(1, 0)))*weights.w;
		t += texture2D(texture, rtex + 4.0*size*(vec2(0, 1)))*weights.w;
		t += texture2D(texture, rtex - 4.0*size*(vec2(0, 1)))*weights.w;
		
		if(t.w == 0.0){
			discard;
		}
		
		c = t.xyz;
		alpha = g_alpha* t.w;
	}
	///////////////////////////////////////////////////////////
	else if ((v_texCoord.x > 0.0) && (rtex.y > 0.0 || rtex.x == 1.0)){
  		vec2 dtx = dFdx(rtex);
  		vec2 dty = dFdy(rtex);
  		
  		rtex.y -= 0.1;
  		
  		if(rtex.y < 0.0) {
  			if(v_texCoord.y < 0.0)
  				discard;
  			else{
  				rtex.y = 0.0;
  			}
  		}
  		
  		vec2 f = vec2((dtx.y - dtx.x + 2.0*rtex.x*dtx.x), (dty.y - dty.x + 2.0*rtex.x*dty.x));
  		float position = rtex.y - (rtex.x * (1.0 - rtex.x));
  		float d = position/(length(f));

		float a = (0.5 - d * sign(v_texCoord.y));  
		
		if (a >= 1.0)  { 
	    	alpha = g_alpha;
    	}  
  		else if (a <= 0.0) {
   			alpha = 0.0;//discard;
   		}
  		else {           
    		alpha = g_alpha*a;
    		mix(b_color,g_color, a);
    	}
	}
	
    gl_FragColor = vec4(c, alpha);
}
