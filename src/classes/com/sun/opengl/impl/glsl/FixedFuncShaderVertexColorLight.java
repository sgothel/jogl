
package com.sun.opengl.impl.glsl;

import javax.media.opengl.util.*;
import javax.media.opengl.*;
import java.nio.*;

public class FixedFuncShaderVertexColorLight {

    public static final String[][] vertShaderSource = new String[][] { {
        "#ifdef GL_ES\n"+
        "  #define MEDIUMP mediump\n"+
        "  #define HIGHP highp\n"+
        "#else\n"+
        "  #define MEDIUMP\n"+
        "  #define HIGHP\n"+
        "#endif\n"+
        "\n"+
        "struct mgl_LightSourceParameters {\n"+
        "   vec4 ambient; \n"+
        "   vec4 diffuse; \n"+
        "   vec4 specular; \n"+
        "   vec4 position; \n"+
        "   // vec4 halfVector; // is computed here\n"+
        "   vec3 spotDirection; \n"+
        "   float spotExponent; \n"+
        "   float spotCutoff; // (range: [0.0,90.0], 180.0)\n"+
        "   //float spotCosCutoff; // (range: [1.0,0.0],-1.0)\n"+
        "   float constantAttenuation; \n"+
        "   float linearAttenuation; \n"+
        "   float quadraticAttenuation; \n"+
        "};\n"+
        "struct mgl_MaterialParameters {\n"+
        "   vec4 ambient;    \n"+
        "   vec4 diffuse;    \n"+
        "   vec4 specular;   \n"+
        "   vec4 emission;   \n"+
        "   float shininess; \n"+
        "};\n"+
        "\n"+
        "\n"+
        "const   MEDIUMP int     MAX_TEXTURE_UNITS = 8; // <=gl_MaxTextureImageUnits \n"+
        "const   MEDIUMP int     MAX_LIGHTS = 8; \n"+
        "\n"+
        "uniform   HIGHP mat4    mgl_PMVMatrix[3]; // P, Mv, and Mvi\n"+
        "uniform   HIGHP mat3    mgl_NormalMatrix; // transpose(inverse(ModelView)).3x3\n"+
        "uniform MEDIUMP int     mgl_ColorEnabled;\n"+
        "uniform   HIGHP vec4    mgl_ColorStatic;\n"+
        "uniform MEDIUMP int     mgl_TexCoordEnabled;\n"+
        "uniform MEDIUMP int     mgl_LightsEnabled;\n"+
        "uniform mgl_LightSourceParameters mgl_LightSource[MAX_LIGHTS];\n"+
        "uniform mgl_MaterialParameters    mgl_FrontMaterial;\n"+
        "\n"+
        "attribute HIGHP vec4    mgl_Vertex;\n"+
        "attribute HIGHP vec3    mgl_Normal;\n"+
        "attribute HIGHP vec4    mgl_Color;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord0;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord1;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord2;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord3;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord4;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord5;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord6;\n"+
        "attribute HIGHP vec4    mgl_MultiTexCoord7;\n"+
        "\n"+
        "varying   HIGHP vec4    frontColor;\n"+
        "varying   HIGHP vec4    mgl_TexCoord[MAX_TEXTURE_UNITS];\n"+
        "\n"+
        "void setTexCoord(in HIGHP vec4 defpos) {\n"+
        "  mgl_TexCoord[0] = ( 0 != (mgl_TexCoordEnabled &   1) ) ? mgl_MultiTexCoord0 : defpos;\n"+
        "  mgl_TexCoord[1] = ( 0 != (mgl_TexCoordEnabled &   2) ) ? mgl_MultiTexCoord1 : defpos;\n"+
        "  mgl_TexCoord[2] = ( 0 != (mgl_TexCoordEnabled &   4) ) ? mgl_MultiTexCoord2 : defpos;\n"+
        "  mgl_TexCoord[3] = ( 0 != (mgl_TexCoordEnabled &   8) ) ? mgl_MultiTexCoord3 : defpos;\n"+
        "  mgl_TexCoord[4] = ( 0 != (mgl_TexCoordEnabled &  16) ) ? mgl_MultiTexCoord4 : defpos;\n"+
        "  mgl_TexCoord[5] = ( 0 != (mgl_TexCoordEnabled &  32) ) ? mgl_MultiTexCoord5 : defpos;\n"+
        "  mgl_TexCoord[6] = ( 0 != (mgl_TexCoordEnabled &  64) ) ? mgl_MultiTexCoord6 : defpos;\n"+
        "  mgl_TexCoord[7] = ( 0 != (mgl_TexCoordEnabled & 128) ) ? mgl_MultiTexCoord7 : defpos;\n"+
        "}\n"+
        "\n"+
        "void main(void)\n"+
        "{\n"+
        "  vec4 position;\n"+
        "  vec3 normal, lightDir, cameraDir, halfDir;\n"+
        "  vec4 ambient, diffuse, specular;\n"+
        "  float NdotL, NdotHV, dist, attenuation;\n"+
        "  int i;\n"+
        "\n"+
        "  position  = mgl_PMVMatrix[1] * mgl_Vertex; // vertex eye position \n"+
        "\n"+
        "  normal = normalize(mgl_NormalMatrix * mgl_Normal); \n"+
        "  // cameraPosition:      (mgl_PMVMatrix[2] * vec4(0,0,0,1.0)).xyz                   \n"+
        "  cameraDir  = normalize( (mgl_PMVMatrix[2] * vec4(0,0,0,1.0)).xyz - mgl_Vertex.xyz ); \n"+
        "\n"+
        "  ambient = vec4(0,0,0,0);\n"+
        "  diffuse = vec4(0,0,0,0);\n"+
        "  specular = vec4(0,0,0,0);\n"+
        "\n"+
        "  for(i=0; i<MAX_LIGHTS; i++) {\n"+
        "    if( 0!= (mgl_LightsEnabled & (1<<i)) ) {\n"+
        "      ambient += mgl_LightSource[i].ambient;\n"+
        "      lightDir = mgl_LightSource[i].position.xyz - position.xyz;\n"+
        "      dist     = length(lightDir);\n"+
        "      lightDir = normalize(lightDir);\n"+
        "      attenuation = 1.0 / ( \n"+
        "                       mgl_LightSource[i].constantAttenuation+ \n"+
        "                       mgl_LightSource[i].linearAttenuation    * dist +   \n"+
        "                       mgl_LightSource[i].quadraticAttenuation * dist * dist );\n"+
        "      NdotL = max(0.0, dot(normal, lightDir));\n"+
        "      diffuse += mgl_LightSource[i].diffuse * NdotL * attenuation;\n"+
        "      if (NdotL != 0.0) {\n"+
        "        halfDir  = normalize (lightDir + cameraDir); \n"+
        "        NdotHV   = max(0.0, dot(normal, halfDir));\n"+
        "        specular += mgl_LightSource[i].specular * \n"+
        "                    pow(NdotHV,gl_FrontMaterial.shininess) * attenuation;\n"+
        "      }\n"+
        "    }\n"+
        "  }\n"+
        "  ambient  += mgl_FrontMaterial.ambient;\n"+
        "  diffuse  *= mgl_FrontMaterial.diffuse;\n"+
        "  specular *= mgl_FrontMaterial.specular;\n"+
        "\n"+
        "  if(mgl_ColorEnabled>0) {\n"+
        "    frontColor=mgl_Color;\n"+
        "  } else {\n"+
        "    frontColor=mgl_ColorStatic;\n"+
        "  }\n"+
        "  if( 0!= mgl_LightsEnabled ) {\n"+
        "    frontColor *= ambient + diffuse + specular;\n"+
        "  }\n"+
        "\n"+
        "  gl_Position  = mgl_PMVMatrix[0] * position;\n"+
        "\n"+
        "  setTexCoord(gl_Position);\n"+
        "}\n" } } ;
}

