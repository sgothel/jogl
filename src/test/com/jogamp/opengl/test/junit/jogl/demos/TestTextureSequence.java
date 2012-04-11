package com.jogamp.opengl.test.junit.jogl.demos;

import java.net.URLConnection;

import javax.media.opengl.GL;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import jogamp.opengl.util.av.NullGLMediaPlayer;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.TextureSequence;

public class TestTextureSequence implements TextureSequence {
    TextureSequence.TextureFrame frame = null;    
    int textureUnit = 0;
    protected int[] texMinMagFilter = { GL.GL_NEAREST, GL.GL_NEAREST };
    protected int[] texWrapST = { GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE };

    public TestTextureSequence() {
    }
    
    public void initGLResources(GL gl) throws GLException {
        if(null == frame) {
            TextureData texData = null;
            try {
                URLConnection urlConn = IOUtil.getResource("jogl/util/data/av/test-ntsc01-160x90.png", NullGLMediaPlayer.class.getClassLoader());
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
    public int getTextureTarget() {
        return frame.getTexture().getTarget();
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
    public TextureSequence.TextureFrame getLastTexture() {
        return frame; // may return null
    }

    @Override
    public TextureSequence.TextureFrame getNextTexture(GL gl, boolean blocking) {
        return frame;
    }
    
}
