
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
     * @see javax.media.opengl.GL#GL_VERTEX_ARRAY
     * @see javax.media.opengl.GL#GL_NORMAL_ARRAY
     * @see javax.media.opengl.GL#GL_COLOR_ARRAY
     * @see javax.media.opengl.GL#GL_TEXTURE_COORD_ARRAY
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
     * @see javax.media.opengl.glsl.ShaderState#glVertexAttribPointer(GL2ES2, GLArrayData)
     */
    public void setLocation(int v);

    /**
     * Determines wheather the data is server side (VBO),
     * or a client side array (false).
     */
    public boolean isVBO();

    /**
     * The offset, if it's an VBO, otherwise -1
     */
    public long getOffset();

    /**
     * The VBO name, if it's an VBO, otherwise -1
     */
    public int getVBOName();

    /**
     * The Buffer holding the data, may be null in case of VBO
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
     * The components size in bytes
     */
    public int getComponentSize();

    /**
     * Return the number of elements.
     */
    public int getElementNumber();

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

