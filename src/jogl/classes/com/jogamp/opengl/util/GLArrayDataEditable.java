
package com.jogamp.opengl.util;

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
    
    public boolean enabled();

    /**
     * Is the buffer written to the VBO ?
     */
    public boolean isVBOWritten();

    /**
     * Marks the buffer written to the VBO
     */
    public void setVBOWritten(boolean written);

    //
    // Data and GL state modification ..
    //

    public void destroy(GL gl);

    public void reset(GL gl);

    /**
     * Convenience method calling {@link #seal(boolean)} and {@link #enableBuffer(GL, boolean)}.
     *
     * @see #seal(boolean)
     * @see #enableBuffer(GL, boolean)
     * 
     */
    public void seal(GL gl, boolean seal);

    /**
     * <p>Enables/disables the buffer, 
     * sets the client state, binds the VBO if used
     * and transfers the data if necessary.</p>
     * 
     * <p>The action will only be executed,
     * if the internal enable state differs, 
     * or 'setEnableAlways' was called with 'true'.</b>
     * 
     * <p>It is up to the user to enable/disable the array properly,
     * ie in case of multiple data sets for the same vertex attribute (VA).
     * Meaning in such case usage of one set while expecting another one
     * to be used for the same VA implies decorating each usage with enable/disable.</p>
     *
     * @see #setEnableAlways(boolean)
     */
    public void enableBuffer(GL gl, boolean enable);

    /**
     * Affects the behavior of 'enableBuffer'.
     *
     * The default is 'false'
     *
     * This is useful when you mix up 
     * GLArrayData usage with conventional GL array calls
     * or in case of a buggy GL VBO implementation.
     *
     * @see #enableBuffer(GL, boolean)
     */
    public void setEnableAlways(boolean always);

    //
    // Data modification ..
    //

    public void reset();

    /**
     * <p>If <i>seal</i> is true, it
     * disables write operations to the buffer.
     * Calls flip, ie limit:=position and position:=0.</p>
     *
     * <p>If <i>seal</i> is false, it
     * enable write operations continuing
     * at the buffer position, where you left off at seal(true),
     * ie position:=limit and limit:=capacity.</p>
     *
     * @see #seal(boolean)
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

