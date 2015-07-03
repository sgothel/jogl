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

package com.jogamp.opengl;

import java.nio.Buffer;

import com.jogamp.opengl.fixedfunc.GLPointerFunc;

/**
 *
 * The total number of bytes hold by the referenced buffer is:
 * getComponentSize()* getComponentNumber() * getElementNumber()
 *
 */
public interface GLArrayData {
    /**
     * Implementation and type dependent object association.
     * <p>
     * One currently known use case is to associate a {@link com.jogamp.opengl.util.glsl.ShaderState ShaderState}
     * to an GLSL aware vertex attribute object, allowing to use the ShaderState to handle it's
     * data persistence, location and state change.<br/>
     * This is implicitly done via {@link com.jogamp.opengl.util.glsl.ShaderState#ownAttribute(GLArrayData, boolean) shaderState.ownAttribute(GLArrayData, boolean)}.
     * </p>
     * @param obj implementation and type dependent association
     * @param enable pass true to enable the association and false to disable it.
     */
    public void associate(Object obj, boolean enable);

    /**
     * Returns true if this data set is intended for a GLSL vertex shader attribute,
     * otherwise false, ie intended for fixed function vertex pointer
     */
    public boolean isVertexAttribute();

    /**
     * The index of the predefined array index, see list below,
     * or -1 in case of a shader attribute array.
     *
     * @see GLPointerFunc#GL_VERTEX_ARRAY
     * @see GLPointerFunc#GL_NORMAL_ARRAY
     * @see GLPointerFunc#GL_COLOR_ARRAY
     * @see GLPointerFunc#GL_TEXTURE_COORD_ARRAY
     */
    public int getIndex();

    /**
     * The name of the reflecting shader array attribute.
     */
    public String getName();

    /**
     * Set a new name for this array.
     * <p>
     * This clears the location, i.e. sets it to -1.
     * </p>
     * @see #setLocation(int)
     * @see #setLocation(GL2ES2, int)
     */
    public void setName(String newName);


    /**
     * Returns the shader attribute location for this name,
     * -1 if not yet determined
     */
    public int getLocation();

    /**
     * Sets the given location of the shader attribute
     *
     * @return the given location
     * @see com.jogamp.opengl.util.glsl.ShaderState#vertexAttribPointer(GL2ES2, GLArrayData)
     */
    public int setLocation(int v);

    /**
     * Retrieves the location of the shader attribute from the linked shader program.
     * <p>
     * No validation is performed within the implementation.
     * </p>
     * @param gl
     * @param program
     * @return &ge;0 denotes a valid attribute location as found and used in the given shader program.
     *         &lt;0 denotes an invalid location, i.e. not found or used in the given shader program.
     */
    public int setLocation(GL2ES2 gl, int program);

    /**
     * Binds the location of the shader attribute to the given location for the unlinked shader program.
     * <p>
     * No validation is performed within the implementation.
     * </p>
     * @param gl
     * @param program
     * @return the given location
     */
    public int setLocation(GL2ES2 gl, int program, int location);

    /**
     * Determines whether the data is server side (VBO) and enabled,
     * or a client side array (false).
     */
    public boolean isVBO();

    /**
     * The VBO buffer offset or 0 if not a VBO
     */
    public long getVBOOffset();

    /**
     * The VBO name or 0 if not a VBO
     */
    public int getVBOName();

    /**
     * The VBO usage or 0 if not a VBO
     * @return 0 if not a GPU buffer, otherwise {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
     */
    public int getVBOUsage();

    /**
     * The VBO target or 0 if not a VBO
     * @return 0 if not a GPU buffer, otherwise {@link GL#GL_ARRAY_BUFFER} or {@link GL#GL_ELEMENT_ARRAY_BUFFER}
     */
    public int getVBOTarget();


    /**
     * The Buffer holding the data, may be null if a GPU buffer without client bound data
     */
    public Buffer getBuffer();

    /**
     * The number of components per element
     */
    public int getComponentCount();

    /**
     * The component's GL data type, ie. GL_FLOAT
     */
    public int getComponentType();

    /**
     * The component's size in bytes
     */
    public int getComponentSizeInBytes();

    /**
     * The current number of used elements.
     * <p>
     * On element consist out of {@link #getComponentCount()} components.
     * </p>
     * In case the buffer's position is 0 (sealed, flipped), it's based on it's limit instead of it's position.
     */
    public int getElementCount();

    /**
     * The currently used size in bytes.<br>
     * In case the buffer's position is 0 (sealed, flipped), it's based on it's limit instead of it's position.
     */
    public int getSizeInBytes();

    /**
     * True, if GL shall normalize fixed point data while converting
     * them into float.
     * <p>
     * Default behavior (of the fixed function pipeline) is <code>true</code>
     * for fixed point data type and <code>false</code> for floating point data types.
     * </p>
     */
    public boolean getNormalized();

    /**
     * @return the byte offset between consecutive components
     */
    public int getStride();

    @Override
    public String toString();

    public void destroy(GL gl);

}

