
package com.sun.opengl.util;

import javax.media.opengl.*;

import java.nio.*;

/**
 *
 * The total number of bytes hold by the referenced buffer is:
 * getComponentSize()* getComponentNumber() * getElementNumber()
 *
 */
public interface GLArrayDataEditable extends GLArrayData {

    public boolean sealed();

    /**
     * The VBO buffer usage, if it's an VBO, otherwise -1
     */
    public int getBufferUsage();

    /**
     * Is the buffer written to the GPU ?
     */
    public boolean isBufferWritten();

    /**
     * Marks the buffer written to the GPU
     */
    public void setBufferWritten(boolean written);

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

    /**
     * Enables/disables the buffer, which implies
     * the client state, binding the VBO
     * and transfering the data if not done yet.
     * 
     * The above will only be executed,
     * if the buffer is disabled,
     * or 'setEnableAlways' was called with 'true'.
     *
     * @see #setEnableAlways(boolean)
     */
    public void enableBuffer(GL gl, boolean enable);

    /**
     * Affects the behavior of 'enableBuffer'.
     *
     * The default is 'false'
     *
     * This is usefull when you mix up 
     * GLArrayData usage with conventional GL array calls.
     *
     * @see #enableBuffer(GL, boolean)
     */
    public void setEnableAlways(boolean always);

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
}

