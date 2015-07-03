/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.GLEventListener;

import com.jogamp.opengl.util.TimeFrameI;

/**
 * Protocol for texture sequences, like animations, movies, etc.
 * <p>
 * Ensure to respect the texture coordinates provided by
 * {@link TextureFrame}.{@link TextureFrame#getTexture() getTexture()}.{@link Texture#getImageTexCoords() getImageTexCoords()}.
 * </p>
 * The user's shader shall be fitted for this implementation.
 * Assuming we use a base shader code w/o headers using </code>ShaderCode</code>.
 * (Code copied from unit test / demo <code>TexCubeES2</code>)
 * <pre>
 *
    static final String[] es2_prelude = { "#version 100\n", "precision mediump float;\n" };
    static final String gl2_prelude = "#version 110\n";
    static final String shaderBasename = "texsequence_xxx";  // the base shader code w/o headers
    static final String myTextureLookupName = "myTexture2D"; // the desired texture lookup function

    private void initShader(GL2ES2 gl, TextureSequence texSeq) {
        // Create & Compile the shader objects
        ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, TexCubeES2.class,
                                            "shader", "shader/bin", shaderBasename, true);
        ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, TexCubeES2.class,
                                            "shader", "shader/bin", shaderBasename, true);

        // Prelude shader code w/ GLSL profile specifics [ 1. pre-proc, 2. other ]
        int rsFpPos;
        if(gl.isGLES2()) {
            // insert ES2 version string in beginning
            rsVp.insertShaderSource(0, 0, es2_prelude[0]);
            rsFpPos = rsFp.insertShaderSource(0, 0, es2_prelude[0]);
        } else {
            // insert GL2 version string in beginning
            rsVp.insertShaderSource(0, 0, gl2_prelude);
            rsFpPos = rsFp.insertShaderSource(0, 0, gl2_prelude);
        }
        // insert required extensions as determined by TextureSequence implementation.
        rsFpPos = rsFp.insertShaderSource(0, rsFpPos, texSeq.getRequiredExtensionsShaderStub());
        if(gl.isGLES2()) {
            // insert ES2 default precision declaration
            rsFpPos = rsFp.insertShaderSource(0, rsFpPos, es2_prelude[1]);
        }
        // negotiate the texture lookup function name
        final String texLookupFuncName = texSeq.getTextureLookupFunctionName(myTextureLookupName);

        // in case a fixed lookup function is being chosen, replace the name in our code
        rsFp.replaceInShaderSource(myTextureLookupName, texLookupFuncName);

        // Cache the TextureSequence shader details in StringBuilder:
        final StringBuilder sFpIns = new StringBuilder();

        // .. declaration of the texture sampler using the implementation specific type
        sFpIns.append("uniform ").append(texSeq.getTextureSampler2DType()).append(" mgl_ActiveTexture;\n");

        // .. the actual texture lookup function, maybe null in case a built-in function is being used
        sFpIns.append(texSeq.getTextureLookupFragmentShaderImpl());

        // Now insert the TextureShader details in our shader after the given tag:
        rsFp.insertShaderSource(0, "TEXTURE-SEQUENCE-CODE-BEGIN", 0, sFpIns);

        // Create & Link the shader program
        ShaderProgram sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);
        if(!sp.link(gl, System.err)) {
            throw new GLException("Couldn't link program: "+sp);
        }
        ...
 * </pre>
 * The above procedure might look complicated, however, it allows most flexibility and
 * workarounds to also deal with GLSL bugs.
 *
 */
public interface TextureSequence {
    public static final String samplerExternalOES = "samplerExternalOES";
    public static final String sampler2D = "sampler2D";

    /**
     * Texture holder interface, maybe specialized by implementation
     * to associated related data.
     */
    public static class TextureFrame extends TimeFrameI {
        public TextureFrame(final Texture t, final int pts, final int duration) {
            super(pts, duration);
            texture = t;
        }
        public TextureFrame(final Texture t) {
            texture = t;
        }

        public final Texture getTexture() { return texture; }

        @Override
        public String toString() {
            return "TextureFrame[pts " + pts + " ms, l " + duration + " ms, texID "+ (null != texture ? texture.getTextureObject() : 0) + "]";
        }
        protected final Texture texture;
    }

    /**
     * Event listener to notify users of updates regarding the {@link TextureSequence}.
     * <p>
     * Implementations sending events down to all listeners,
     * while not necessarily making the user's OpenGL context current.
     * </p>
     * <p>
     * Events may be sent from a 3rd-party thread, possibly holding another, maybe shared, OpenGL context current.<br/>
     * Hence a user shall not issue <i>any</i> OpenGL, time consuming
     * or {@link TextureSequence} operations directly.<br>
     * Instead, the user shall:
     * <ul>
     *   <li>off-load complex or {@link TextureSequence} commands on another thread, or</li>
     *   <li>injecting {@link GLRunnable} objects via {@link GLAutoDrawable#invoke(boolean, GLRunnable)}, or</li>
     *   <li>simply changing a volatile state of their {@link GLEventListener} implementation.</li>
     * </ul>
     * </p>
     * */
    public interface TexSeqEventListener<T extends TextureSequence> {
        /**
         * Signaling listeners that a new {@link TextureFrame} is available.
         * <p>
         * User shall utilize {@link TextureSequence#getNextTexture(GL)} to dequeue it to maintain
         * a consistent queue.
         * </p>
         * @param ts the event source
         * @param newFrame the newly enqueued frame
         * @param when system time in msec.
         **/
        public void newFrameAvailable(T ts, TextureFrame newFrame, long when);
    }

    /** Returns the texture target used by implementation. */
    public int getTextureTarget();

    /** Return the texture unit used to render the current frame. */
    public int getTextureUnit();

    public int[] getTextureMinMagFilter();

    public int[] getTextureWrapST();

    /**
     * Returns true if texture source is ready <i>and</i> a texture is available
     * via {@link #getNextTexture(GL)} and {@link #getLastTexture()}.
     */
    public boolean isTextureAvailable();

    /**
     * Returns the last updated texture.
     * <p>
     * In case the instance is just initialized, it shall return a <code>TextureFrame</code>
     * object with valid attributes. The texture content may be undefined
     * until the first call of {@link #getNextTexture(GL)}.<br>
     * </p>
     * Not blocking.
     *
     * @throws IllegalStateException if instance is not initialized
     */
    public TextureFrame getLastTexture() throws IllegalStateException ;

    /**
     * Returns the next texture to be rendered.
     * <p>
     * Implementation shall return the next frame if available, may block if a next frame may arrive <i>soon</i>.
     * Otherwise implementation shall return the last frame.
     * </p>
     * <p>
     * Shall return <code>null</code> in case <i>no</i> next or last frame is available.
     * </p>
     *
     * @throws IllegalStateException if instance is not initialized
     */
    public TextureFrame getNextTexture(GL gl) throws IllegalStateException ;

    /**
     * In case a shader extension is required, based on the implementation
     * and the runtime GL profile, this method returns the preprocessor macros, e.g.:
     * <pre>
     * #extension GL_OES_EGL_image_external : enable
     * </pre>
     *
     * @throws IllegalStateException if instance is not initialized
     */
    public String getRequiredExtensionsShaderStub() throws IllegalStateException ;

    /**
     * Returns either <code>sampler2D</code> or <code>samplerExternalOES</code>
     * depending on {@link #getLastTexture()}.{@link TextureFrame#getTexture() getTexture()}.{@link Texture#getTarget() getTarget()}.
     *
     * @throws IllegalStateException if instance is not initialized
     **/
    public String getTextureSampler2DType() throws IllegalStateException ;

    /**
     * @param desiredFuncName desired lookup function name. If <code>null</code> or ignored by the implementation,
     *                        a build-in name is returned.
     * @return the final lookup function name
     *
     * @see {@link #getTextureLookupFragmentShaderImpl()}
     *
     * @throws IllegalStateException if instance is not initialized
     */
    public String getTextureLookupFunctionName(String desiredFuncName) throws IllegalStateException ;

    /**
     * Returns the complete texture2D lookup function code of type
     * <pre>
     *   vec4 <i>funcName</i>(in <i>getTextureSampler2DType()</i> image, in vec2 texCoord) {
     *      vec4 texColor = do_something_with(image, texCoord);
     *      return texColor;
     *   }
     * </pre>
     * <p>
     * <i>funcName</i> can be negotiated and queried via {@link #getTextureLookupFunctionName(String)}.
     * </p>
     * Note: This function may return an empty string in case a build-in lookup
     * function is being chosen. If the implementation desires so,
     * {@link #getTextureLookupFunctionName(String)} will ignore the desired function name
     * and returns the build-in lookup function name.
     * </p>
     * @see #getTextureLookupFunctionName(String)
     * @see #getTextureSampler2DType()
     *
     * @throws IllegalStateException if instance is not initialized
     */
    public String getTextureLookupFragmentShaderImpl() throws IllegalStateException;

    /**
     * Returns the hash code of the strings:
     * <ul>
     *   <li>{@link #getTextureLookupFragmentShaderImpl()}</li>
     *   <li>{@link #getTextureSampler2DType()}</li>
     * </ul>
     * <p>
     * Returns zero if {@link #isTextureAvailable() texture is not available}.
     * </p>
     * The returned hash code allows selection of a matching shader program for this {@link TextureSequence} instance.
     * <p>
     * </p>
     * <p>
     * Implementation shall cache the resulting hash code,
     * which must be reset to zero if {@link #isTextureAvailable() texture is not available}.
     * </p>
     */
    public int getTextureFragmentShaderHashCode();
}
