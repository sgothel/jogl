
package javax.media.opengl;

import java.nio.*;

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


    public boolean sealed();

    /**
     * The offset, if it's an VBO, otherwise -1
     */
    public long getOffset();

    /**
     * The Buffer holding the data, may be null in case of VBO
     */
    public Buffer getBuffer();

    /**
     * Determines wheather the data is server side (VBO),
     * or a client side array (false).
     */
    public boolean isVBO();

    /**
     * The number of components per element
     */
    public int getComponents();

    /**
     * The GL data type of the components, ie. GL_FLOAT
     */
    public int getDataType();

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

    public int getVerticeNumber();

    public int getBufferCompSize();

    public String toString();

    //
    // Data and GL state modification ..
    //

    public void destroy(GL gl);

    public void reset(GL gl);

    /**
     * If seal is true, it
     * disable write operations to the buffer.
     * Calls flip, ie limit:=position and position:=0.
     * Also enables the buffer for OpenGL, and passes the data.
     *
     * If seal is false, it
     * enable write operations continuing
     * at the buffer position, where you left off at seal(true),
     * ie position:=limit and limit:=capacity.
     * Also disables the buffer for OpenGL.
     *
     * @see #seal(boolean)
     */
    public void seal(GL gl, boolean seal);

    public void enableBuffer(GL gl, boolean enable);

    //
    // Data modification ..
    //

    public void reset();

    /**
     * If seal is true, it
     * disable write operations to the buffer.
     * Calls flip, ie limit:=position and position:=0.
     *
     * If seal is false, it
     * enable write operations continuing
     * at the buffer position, where you left off at seal(true),
     * ie position:=limit and limit:=capacity.
     *
     */
    public void seal(boolean seal);

    public void rewind();
    public void padding(int done);
    public void put(Buffer v);
    public void putb(byte v);
    public void puts(short v);
    public void puti(int v);
    public void putx(int v);
    public void putf(float v);
    public void putd(double v);

}

