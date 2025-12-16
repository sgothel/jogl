/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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
     * This is implicitly done via {@link com.jogamp.opengl.util.glsl.ShaderState#manage(GLArrayData, boolean) shaderState.ownAttribute(GLArrayData, boolean)}.
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
     * @see #resolveLocation(GL2ES2, int)
     */
    public void setName(String newName);


    /**
     * Returns the shader attribute location for this name,
     * -1 if not yet determined
     */
    public int getLocation();

    /** Returns true is location() is >= 0, otherwise false */
    public boolean hasLocation();

    /**
     * Sets the given location of the shader attribute
     *
     * @see com.jogamp.opengl.util.glsl.ShaderState#vertexAttribPointer(GL2ES2, GLArrayData)
     */
    public void setLocation(int v);

    /**
     * Retrieves the location of the shader attribute from the linked shader program.
     * <p>
     * No validation is performed within the implementation.
     * </p>
     * @param gl
     * @param program
     * @return true if a valid attribute location has been found and used in the given shader program, otherwise false.
     */
    public boolean resolveLocation(GL2ES2 gl, int program);

    /**
     * Binds the location of the shader attribute to the given location for the unlinked shader program.
     * <p>
     * No validation is performed within the implementation.
     * </p>
     * @param gl
     * @param program
     */
    public void setLocation(GL2ES2 gl, int program, int location);

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
    public int getCompsPerElem();

    /**
     * The component's GL data type, ie. GL_FLOAT
     */
    public int getCompType();

    /**
     * The component's size in bytes
     */
    public int getBytesPerComp();

    /**
     * Returns true if data has been {@link com.jogamp.opengl.util.GLArrayDataEditable#seal(boolean) sealed} (flipped to read), otherwise false (writing mode).
     *
     * @see com.jogamp.opengl.util.GLArrayDataEditable#seal(boolean)
     * @see com.jogamp.opengl.util.GLArrayDataEditable#seal(GL, boolean)
     */
    public boolean sealed();

    /**
     * Returns the element position (written elements) if not {@link #sealed()} or
     * the element limit (available to read) after {@link #sealed()} (flip).
     * <p>
     * On element consist out of {@link #getCompsPerElem()} components.
     * </p>
     * @see #sealed()
     * @see #getByteCount()
     * @see #elemPosition()
     * @see #remainingElems()
     * @see #getElemCapacity()
     */
    public int getElemCount();

    /**
     * Returns the element position.
     * <p>
     * On element consist out of {@link #getCompsPerElem()} components.
     * </p>
     * @see #bytePosition()
     * @see #getElemCount()
     * @see #remainingElems()
     * @see #getElemCapacity()
     */
    public int elemPosition();

    /**
     * The current number of remaining elements.
     * <p>
     * On element consist out of {@link #getCompsPerElem()} components.
     * </p>
     * Returns the number of elements between the current position and the limit, i.e. remaining elements to write in this buffer.
     * @see #remainingBytes()
     * @see #getElemCount()
     * @see #elemPosition()
     * @see #getElemCapacity()
     */
    public int remainingElems();

    /**
     * Return the element capacity.
     * <p>
     * On element consist out of {@link #getCompsPerElem()} components.
     * </p>
     * @see #getByteCapacity()
     * @see #getElemCount()
     * @see #elemPosition()
     * @see #remainingElems()
     */
    public int getElemCapacity();

    /**
     * Returns the byte position (written elements) if not {@link #sealed()} or
     * the byte limit (available to read) after {@link #sealed()} (flip).
     * @see #sealed()
     * @see #getElemCount()
     * @see #bytePosition()
     * @see #remainingBytes()
     * @see #getByteCapacity()
     */
    public int getByteCount();

    /**
     * Returns the bytes position.
     * @see #elemPosition()
     * @see #getByteCount()
     * @see #remainingElems()
     * @see #getElemCapacity()
     */
    public int bytePosition();

    /**
     * The current number of remaining bytes.
     * <p>
     * Returns the number of bytes between the current position and the limit, i.e. remaining bytes to write in this buffer.
     * </p>
     * @see #remainingElems()
     * @see #getByteCount()
     * @see #bytePosition()
     * @see #getByteCapacity()
     */
    public int remainingBytes();

    /**
     * Return the capacity in bytes.
     * @see #getElemCapacity()
     * @see #getByteCount()
     * @see #bytePosition()
     * @see #remainingBytes()
     */
    public int getByteCapacity();

    /** Returns a string with detailed buffer fill stats. */
    public String fillStatsToString();
    /** Returns a string with detailed buffer element stats, i.e. sealed, count, position, remaining, limit and capacity.  */
    public String elemStatsToString();

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

