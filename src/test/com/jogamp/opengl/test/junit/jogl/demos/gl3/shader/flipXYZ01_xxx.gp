// Copyright 2012 JogAmp Community. All rights reserved.
// Requires version >= 150

layout (triangles) in;
layout (triangle_strip, max_vertices=3) out;

in VertexData {
    vec4 frontColor;
    vec2 texCoord;
} vp_data[3];

out VertexData {
    vec4 frontColor;
    vec2 texCoord;
} gp_data;
 
void main() 
{ 
  for(int i = 0; i < gl_in.length(); i++)
  {
     // copy attributes
    gl_Position = vec4(gl_in[i].gl_Position.xyz*-1,1); // This line flips the coordinates.
    gp_data.frontColor = vp_data[i].frontColor;
    gp_data.texCoord = vp_data[i].texCoord;
 
    // done with the vertex
    EmitVertex();
  }
} 



