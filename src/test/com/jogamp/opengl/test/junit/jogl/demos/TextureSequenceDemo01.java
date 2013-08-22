package com.jogamp.opengl.test.junit.jogl.demos;

import java.net.URLConnection;

import javax.media.opengl.GL;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.TextureSequence;

public class TextureSequenceDemo01 implements TextureSequence {
    TextureSequence.TextureFrame frame = null;    
    int textureUnit = 0;
    protected int[] texMinMagFilter = { GL.GL_NEAREST, GL.GL_NEAREST };
    protected int[] texWrapST = { GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE };
    final boolean useBuildInTexLookup;
    
    public TextureSequenceDemo01(boolean useBuildInTexLookup) {
        this.useBuildInTexLookup = useBuildInTexLookup;
    }
    
    public void initGLResources(GL gl) throws GLException {
        if(null == frame) {
            TextureData texData = null;
            try {
                URLConnection urlConn = IOUtil.getResource("jogl/util/data/av/test-ntsc01-160x90.png", this.getClass().getClassLoader());
                if(null != urlConn) {
                    texData = TextureIO.newTextureData(GLProfile.getGL2ES2(), urlConn.getInputStream(), false, TextureIO.PNG);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            final Texture tex = new Texture(gl, texData);
            frame = new TextureSequence.TextureFrame(tex);
            tex.bind(gl);
            gl.glTexParameteri(tex.getTarget(), GL.GL_TEXTURE_MIN_FILTER, texMinMagFilter[0]);
            gl.glTexParameteri(tex.getTarget(), GL.GL_TEXTURE_MAG_FILTER, texMinMagFilter[1]);        
            gl.glTexParameteri(tex.getTarget(), GL.GL_TEXTURE_WRAP_S, texWrapST[0]);
            gl.glTexParameteri(tex.getTarget(), GL.GL_TEXTURE_WRAP_T, texWrapST[1]);
        }
    }
    
    public void destroyGLResources(GL gl) {
        if(null != frame) {
            frame.getTexture().destroy(gl);
            frame = null;
        }
    }
    
    public void destroy(GL gl) throws GLException {
        frame.getTexture().destroy(gl);
        frame = null;        
    }
    
    @Override
    public int getTextureUnit() {
        return textureUnit;
    }

    @Override
    public int[] getTextureMinMagFilter() {
        return texMinMagFilter;
    }

    @Override
    public int[] getTextureWrapST() {
        return texWrapST;
    }

    @Override
    public TextureSequence.TextureFrame getLastTexture() throws IllegalStateException {
        return frame; // may return null
    }

    @Override
    public TextureSequence.TextureFrame getNextTexture(GL gl) throws IllegalStateException {
        return frame;
    }
    
    @Override
    public String getRequiredExtensionsShaderStub() throws IllegalStateException {
        return "// TextTextureSequence: No extensions required\n";
    }
    
    @Override
    public String getTextureSampler2DType() throws IllegalStateException {
        return "sampler2D" ;
    }    
    
    private String textureLookupFunctionName = "myTexture2D";
    
    @Override
    public String getTextureLookupFunctionName(String desiredFuncName) throws IllegalStateException {
        if(useBuildInTexLookup) {
            return "texture2D";
        }
        if(null != desiredFuncName && desiredFuncName.length()>0) {
            textureLookupFunctionName = desiredFuncName;
        }
        return textureLookupFunctionName;
    }
    
    @Override
    public String getTextureLookupFragmentShaderImpl() throws IllegalStateException {
        if(useBuildInTexLookup) {
          return "";
        }
        return
          "\n"+
          "vec4 "+textureLookupFunctionName+"(in "+getTextureSampler2DType()+" image, in vec2 texCoord) {\n"+
          "  return texture2D(image, texCoord);\n"+
          "}\n\n";
    }
}
