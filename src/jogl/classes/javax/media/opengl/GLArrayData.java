/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

package javax.media.opengl;

import java.nio.*;

/**
 *
 * The total number of bytes hold by the referenced buffer is:
 * getComponentSize()* getComponentNumber() * getElementNumber()
 *
 */
public interface GLArrayData {
    /**
     * Returns true if this data set is intended for a GLSL vertex shader attribute,
     * otherwise false, ie intended for fixed function vertex pointer
     */
    public boolean isVertexAttribute();

    /**
     * The index of the predefined array index, see list below,
     * or -1 in case of a shader attribute array.
     *
     * @see javax.media.opengl.GL2#GL_VERTEX_ARRAY
     * @see javax.media.opengl.GL2#GL_NORMAL_ARRAY
     * @see javax.media.opengl.GL2#GL_COLOR_ARRAY
     * @see javax.media.opengl.GL2#GL_TEXTURE_COORD_ARRAY
     */
    public int getIndex();

    /**
     * The name of the reflecting shader array attribute.
     */
    public String getName();

    /**
     * Set a new name for this array.
     */
    public void setName(String newName);


    /**
     * Returns the shader attribute location for this name,
     * -1 if not yet determined
     */
    public int getLocation();

    /**
     * Sets the determined location of the shader attribute
     * This is usually done within ShaderState.
     *
     * @see com.jogamp.opengl.util.glsl.ShaderState#vertexAttribPointer(GL2ES2, GLArrayData)
     */
    public void setLocation(int v);

    /**
     * Determines whether the data is server side (VBO) and enabled,
     * or a client side array (false).
     */
    public boolean isVBO();

    /**
     * The VBO buffer offset or -1 if not a VBO
     */
    public long getVBOOffset();

    /**
     * The VBO name or -1 if not a VBO
     */
    public int getVBOName();

    /**
     * The VBO usage or -1 if not a VBO
     * @return -1 if not a GPU buffer, otherwise {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
     */
    public int getVBOUsage();

    /**
     * The VBO target or -1 if not a VBO
     * @return -1 if not a GPU buffer, otherwise {@link GL#GL_ARRAY_BUFFER} or {@link GL#GL_ELEMENT_ARRAY_BUFFER}
     */
    public int getVBOTarget();

    
    /**
     * The Buffer holding the data, may be null if a GPU buffer without client bound data
     */
    public Buffer getBuffer();

    /**
     * The number of components per element
     */
    public int getComponentNumber();

    /**
     * The component's GL data type, ie. GL_FLOAT
     */
    public int getComponentType();

    /**
     * The component's size in bytes
     */
    public int getComponentSize();

    /**
     * The current number of used elements.<br>
     * In case the buffer's position is 0 (sealed, flipped), it's based on it's limit instead of it's position.
     */
    public int getElementNumber();
    
    /**
     * The current number of used bytes.<br>
     * In case the buffer's position is 0 (sealed, flipped), it's based on it's limit instead of it's position.
     */
    public int getByteSize();

    /**
     * True, if GL shall normalize fixed point data while converting 
     * them into float
     */
    public boolean getNormalized();

    /**
     * The distance to the next payload,
     * allowing interleaved arrays.
     */
    public int getStride();

    public String toString();

    public void destroy(GL gl);

}

