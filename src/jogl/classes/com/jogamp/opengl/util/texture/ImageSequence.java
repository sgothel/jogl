/**
 * Copyright 2014-2024 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.opengl.util.texture;

import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import com.jogamp.common.util.IOUtil;
import com.jogamp.math.Vec4f;

/**
 * Simple {@link TextureSequence} implementation
 * allowing {@link #addFrame(GL, Texture) existing textures}
 * or {@link #addFrame(GL, Class, String, String) image streams}
 * to be used and <i>replayed</i> as {@link TextureSequence.TextureFrame frames}.
 */
public class ImageSequence implements TextureSequence {
    private final int textureUnit;
    private final boolean useBuildInTexLookup;
    private final List<TextureSequence.TextureFrame> frames = new ArrayList<TextureSequence.TextureFrame>();
    private final int[] texMinMagFilter = { GL.GL_NEAREST, GL.GL_NEAREST };
    private final int[] texWrapST = { GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE };
    private volatile int frameIdx = 0;
    private volatile boolean manualStepping = false;
    private int textureFragmentShaderHashCode = 0;
    private boolean aRatioAdjustment = true;
    private boolean aRatioLbox = false;
    private final Vec4f aRatioLboxBackColor = new Vec4f();

    public ImageSequence(final int textureUnit, final boolean useBuildInTexLookup) {
        this.textureUnit = textureUnit;
        this.useBuildInTexLookup = useBuildInTexLookup;
    }

    public void setParams(final int magFilter, final int minFilter, final int wrapS, final int wrapT) {
        texMinMagFilter[0] = minFilter;
        texMinMagFilter[1] = magFilter;
        texWrapST[0] = wrapS;
        texWrapST[1] = wrapT;
    }

    public final TextureSequence.TextureFrame addFrame(final GL gl, final Texture tex) {
        return addFrame(gl, new TextureSequence.TextureFrame(tex));
    }
    public final TextureSequence.TextureFrame addFrame(final GL gl, final TextureSequence.TextureFrame frame) {
        frames.add(frame);
        frame.texture.bind(gl);
        gl.glTexParameteri(getTextureTarget(), GL.GL_TEXTURE_MIN_FILTER, texMinMagFilter[0]);
        gl.glTexParameteri(getTextureTarget(), GL.GL_TEXTURE_MAG_FILTER, texMinMagFilter[1]);
        gl.glTexParameteri(getTextureTarget(), GL.GL_TEXTURE_WRAP_S, texWrapST[0]);
        gl.glTexParameteri(getTextureTarget(), GL.GL_TEXTURE_WRAP_T, texWrapST[1]);
        return frame;
    }
    public boolean removeFrame(final TextureFrame tex) {
        return frames.remove(tex);
    }
    public void removeAllFrames() {
        frames.clear();
    }

    public final void addFrame(final GL gl, final Class<?> context, final String imageResourcePath, final String imageSuffix) throws IOException {
        final URLConnection urlConn = IOUtil.getResource(imageResourcePath, context.getClassLoader(), context);
        if(null != urlConn) {
            final TextureData texData = TextureIO.newTextureData(GLProfile.getGL2ES2(), urlConn.getInputStream(), false, imageSuffix);
            final Texture tex = new Texture(getTextureTarget());
            tex.updateImage(gl, texData);
            addFrame(gl, tex);
        }
    }
    public final int getFrameCount() { return frames.size(); }
    public final int getCurrentIdx() { return frameIdx; }
    public final void setCurrentIdx(final int idx) throws IndexOutOfBoundsException {
        if( 0 > idx || idx >= frames.size() ) {
            throw new IndexOutOfBoundsException("idx shall be within 0 <= "+idx+" < "+frames.size());
        }
        frameIdx=idx;
    }
    public final void setManualStepping(final boolean v) { manualStepping = v; }
    public final boolean isManualStepping() { return manualStepping; }

    /** Returns {@code true} if not {@link #isManualStepping()} and {@link #getFrameCount()} > 1 */
    public final boolean isSequenceAnimating() { return !manualStepping && frames.size() > 1; }
    public final TextureSequence.TextureFrame getFrame(final int idx) { return frames.get(idx); }

    public void destroy(final GL gl) throws GLException {
        for(int i=frames.size()-1; i>=0; i--) {
            frames.get(i).getTexture().destroy(gl);
        }
        frames.clear();
    }

    @Override
    public int getTextureTarget() {
        return GL.GL_TEXTURE_2D;
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

    /**
     * {@inheritDoc}
     * <p>
     * Defaults to {@code true} and toggling is supported via {@link #setARatioAdjustment(boolean)}
     * </p>
     */
    @Override
    public boolean useARatioAdjustment() { return aRatioAdjustment; }

    /**
     * {@inheritDoc}
     * <p>
     * Defaults to {@code true}.
     * </p>
     */
    @Override
    public void setARatioAdjustment(final boolean v) { aRatioAdjustment = v; }

    /**
     * {@inheritDoc}
     * <p>
     * Defaults to {@code false} and toggling is supported via {@link #setARatioLetterbox(boolean, Vec4f)}
     * </p>
     */
    @Override
    public boolean useARatioLetterbox() { return aRatioLbox; }

    @Override
    public Vec4f getARatioLetterboxBackColor() { return aRatioLboxBackColor; }

    /**
     * {@inheritDoc}
     * <p>
     * Defaults to {@code false}.
     * </p>
     */
    @Override
    public void setARatioLetterbox(final boolean v, final Vec4f backColor) {
        aRatioLbox = v;
        if( null != backColor ) {
            aRatioLboxBackColor.set(backColor);
        }
    };

    @Override
    public boolean isTextureAvailable() { return frames.size() > 0; }

    @Override
    public TextureSequence.TextureFrame getLastTexture() throws IllegalStateException {
        return frames.get(frameIdx); // may return null
    }

    @Override
    public TextureSequence.TextureFrame getNextTexture(final GL gl) throws IllegalStateException {
        if( !manualStepping ) {
            frameIdx = ( frameIdx + 1 ) % frames.size();
        }
        return frames.get(frameIdx);
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
    public String setTextureLookupFunctionName(final String texLookupFuncName) throws IllegalStateException {
        if(useBuildInTexLookup) {
            textureLookupFunctionName = "texture2D";
        } else if(null != texLookupFuncName && texLookupFuncName.length()>0) {
            textureLookupFunctionName = texLookupFuncName;
        }
        textureFragmentShaderHashCode = 0;
        return textureLookupFunctionName;
    }

    @Override
    public String getTextureLookupFunctionName() throws IllegalStateException {
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

    @Override
    public String getTextureFragmentShaderHashID() {
        // return getTextureSampler2DType()+";"+getTextureLookupFunctionName()+";"+getTextureLookupFragmentShaderImpl();
        if( useBuildInTexLookup ) {
            return getTextureSampler2DType()+";"+getTextureLookupFunctionName();
        } else {
            return getTextureLookupFragmentShaderImpl();
        }
    }

    @Override
    public int getTextureFragmentShaderHashCode() {
        if( !isTextureAvailable() ) {
            textureFragmentShaderHashCode = 0;
            return 0;
        } else if( 0 == textureFragmentShaderHashCode ) {
            final int hash = getTextureFragmentShaderHashID().hashCode();
            textureFragmentShaderHashCode = hash;
        }
        return textureFragmentShaderHashCode;
    }
}
