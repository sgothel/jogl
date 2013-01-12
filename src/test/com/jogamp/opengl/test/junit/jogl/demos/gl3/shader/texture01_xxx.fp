// Copyright 2012 JogAmp Community. All rights reserved.
// Requires version >= 130

in VertexData {
    vec4 frontColor;
    vec2 texCoord;
} gp_data;

out vec4 mgl_FragColor;

uniform sampler2D mgl_ActiveTexture;

void main (void)
{
  vec4 texColor = texture(mgl_ActiveTexture, gp_data.texCoord);

  // mix frontColor with texture ..
  mgl_FragColor = vec4(gp_data.frontColor*texColor);
}

