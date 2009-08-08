
package com.sun.opengl.util.glsl;

import javax.media.opengl.*;
import com.sun.opengl.util.*;
import com.sun.opengl.impl.Debug;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.nio.*;
import java.io.PrintStream;
import java.security.*;

public class ShaderState {
    public static final boolean DEBUG = Debug.isPropertyDefined("jogl.debug.GLSLState", true, AccessController.getContext());

    public ShaderState() {
    }

    public boolean verbose() { return verbose; }

    public void setVerbose(boolean v) { verbose=v; }

    /**
     * Fetches the current shader state from the thread local storage (TLS)
     *
     * @see javax.media.opengl.glsl.ShaderState#glUseProgram(GL2ES2, boolean)
     * @see javax.media.opengl.glsl.ShaderState#getCurrent()
     */
    public static synchronized ShaderState getCurrent() { 
        GLContext current = GLContext.getCurrent();
        if(null==current) {
            throw new GLException("No context is current on this thread");
        }
        return (ShaderState) current.getAttachedObject(ShaderState.class.getName());
    }

    /**
     * Turns the shader program on or off.<br>
     * Puts this ShaderState to to the thread local storage (TLS),
     * if <code>on</code> is <code>true</code>.
     *
     * @see javax.media.opengl.glsl.ShaderState#glUseProgram(GL2ES2, boolean)
     * @see javax.media.opengl.glsl.ShaderState#getCurrent()
     */
    public synchronized void glUseProgram(GL2ES2 gl, boolean on) {
        if(on) {
            if(null!=shaderProgram) {
                shaderProgram.glUseProgram(gl, true);
            } else {
                throw new GLException("No program is attached");
            }
            // update the current ShaderState to the TLS ..
            gl.getContext().putAttachedObject(ShaderState.class.getName(), this);
        } else if(null!=shaderProgram) {
            shaderProgram.glUseProgram(gl, false);
        }
    }

    public boolean linked() {
        return (null!=shaderProgram)?shaderProgram.linked():false;
    }

    public boolean inUse() {
        return (null!=shaderProgram)?shaderProgram.inUse():false;
    }

    /**
     * Attach or switch a shader program
     *
     * Attaching a shader program the first time, 
     * as well as switching to another program on the fly,
     * while managing all attribute and uniform data.
     */
    public synchronized void attachShaderProgram(GL2ES2 gl, ShaderProgram prog) {
        boolean prgInUse = false; // earmarked state

        if(DEBUG) {
            int curId = (null!=shaderProgram)?shaderProgram.id():-1;
            int newId = (null!=prog)?prog.id():-1;
            System.err.println("Info: attachShaderProgram: "+curId+" -> "+newId+"\n\t"+shaderProgram+"\n\t"+prog);
            if(verbose) {
                Throwable tX = new Throwable("Info: attachShaderProgram: Trace");
                tX.printStackTrace();
            }
        }
        if(null!=shaderProgram) {
            if(shaderProgram.equals(prog)) {
                // nothing to do ..
                if(DEBUG) {
                    System.err.println("Info: attachShaderProgram: NOP: equal id: "+shaderProgram.id());
                }
                return;
            }
            prgInUse = shaderProgram.inUse();
            shaderProgram.glUseProgram(gl, false);
        }

        // register new one
        shaderProgram = prog;

        if(null!=shaderProgram) {
            // reinstall all data ..
            shaderProgram.glUseProgram(gl, true);
            glResetAllVertexAttributes(gl);
            glResetAllUniforms(gl);
            if(!prgInUse) {
                shaderProgram.glUseProgram(gl, false);
            }
        }
        if(DEBUG) {
            System.err.println("Info: attachShaderProgram: END");
        }
    }

    public ShaderProgram shaderProgram() { return shaderProgram; }

    /**
     * Calls release(gl, true, true)
     *
     * @see #glReleaseAllVertexAttributes
     * @see #glReleaseAllUniforms
     * @see #release(GL2ES2, boolean, boolean)
     */
    public synchronized void destroy(GL2ES2 gl) {
        release(gl, true, true);
    }

    /**
     * Calls release(gl, false, false)
     *
     * @see #glReleaseAllVertexAttributes
     * @see #glReleaseAllUniforms
     * @see #release(GL2ES2, boolean, boolean)
     */
    public synchronized void releaseAllData(GL2ES2 gl) {
        release(gl, false, false);
    }

    /**
     * @see #glReleaseAllVertexAttributes
     * @see #glReleaseAllUniforms
     * @see ShaderProgram#release(GL2ES2, boolean)
     */
    public synchronized void release(GL2ES2 gl, boolean releaseProgramToo, boolean releaseShaderToo) {
        boolean prgInUse = false;
        if(null!=shaderProgram) {
            prgInUse = shaderProgram.inUse();
            if(!prgInUse) {
                shaderProgram.glUseProgram(gl, true);
            }
        }
        glReleaseAllVertexAttributes(gl);
        glReleaseAllUniforms(gl);
        if(null!=shaderProgram) {
            if(releaseProgramToo) {
                shaderProgram.release(gl, releaseShaderToo);
            } else if(!prgInUse) {
                shaderProgram.glUseProgram(gl, false);
            }
        }
    }

    //
    // Shader attribute handling
    //

    /**
     * Binds an attribute to the shader.
     * This must be done before the program is linked !
     * n name - 1 idx, where name is a uniq key
     *
     * @throws GLException is the program is already linked
     *
     * @see #glBindAttribLocation
     * @see javax.media.opengl.GL2ES2#glBindAttribLocation
     * @see #glGetAttribLocation
     * @see javax.media.opengl.GL2ES2#glGetAttribLocation
     * @see #getAttribLocation
     * @see #glReplaceShader
     */
    public void glBindAttribLocation(GL2ES2 gl, int index, String name) {
        if(null==shaderProgram) throw new GLException("No program is attached");
        if(shaderProgram.linked()) throw new GLException("Program is already linked");
        Integer idx = new Integer(index);
        if(!attribMap2Idx.containsKey(name)) {
            attribMap2Idx.put(name, idx);
            gl.glBindAttribLocation(shaderProgram.program(), index, name);
        }
    }

    /**
     * Gets the index of a shader attribute.
     * This must be done after the program is linked !
     *
     * @return -1 if there is no such attribute available, 
     *         otherwise >= 0
     * @throws GLException is the program is not linked
     *
     * @see #glBindAttribLocation
     * @see javax.media.opengl.GL2ES2#glBindAttribLocation
     * @see #glGetAttribLocation
     * @see javax.media.opengl.GL2ES2#glGetAttribLocation
     * @see #getAttribLocation
     * @see #glReplaceShader
     */
    public int glGetAttribLocation(GL2ES2 gl, String name) {
        if(!shaderProgram.linked()) throw new GLException("Program is not linked");
        int index = getAttribLocation(name);
        if(0>index) {
            index = gl.glGetAttribLocation(shaderProgram.program(), name);
            if(0<=index) {
                Integer idx = new Integer(index);
                attribMap2Idx.put(name, idx);
                if(DEBUG) {
                    System.err.println("Info: glGetAttribLocation: "+name+", loc: "+index);
                }
            } else if(verbose) {
                Throwable tX = new Throwable("Info: glGetAttribLocation failed, no location for: "+name+", index: "+index);
                tX.printStackTrace();
            }
        }
        return index;
    }

    protected int getAttribLocation(String name) {
        Integer idx = (Integer) attribMap2Idx.get(name);
        return (null!=idx)?idx.intValue():-1;
    }


    //
    // Enabled Vertex Arrays and its data
    //

    /**
     * Enable a vertex attribute array
     *
     * Even if the attribute is not found in the current shader,
     * it is stored in this state.
     *
     * @returns false, if the name is not found, otherwise true
     *
     * @throws GLException if the program is not in use
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttributePointer
     * @see #glReleaseAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see #glReplaceShader
     */
    public boolean glEnableVertexAttribArray(GL2ES2 gl, String name) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        enabledVertexAttribArraySet.add(name);
        int index = glGetAttribLocation(gl, name);
        if(0>index) {
            if(verbose) {
                Throwable tX = new Throwable("Info: glEnableVertexAttribArray failed, no index for: "+name);
                tX.printStackTrace();
            }
            return false;
        }
        if(DEBUG) {
            System.err.println("Info: glEnableVertexAttribArray: "+name+", loc: "+index);
        }
        gl.glEnableVertexAttribArray(index);
        return true;
    }

    public boolean isVertexAttribArrayEnabled(String name) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        return enabledVertexAttribArraySet.contains(name);
    }

    /**
     * Disables a vertex attribute array
     *
     * Even if the attribute is not found in the current shader,
     * it is removed from this state.
     *
     * @returns false, if the name is not found, otherwise true
     *
     * @throws GLException if the program is not in use
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttributePointer
     * @see #glReleaseAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see #glReplaceShader
     */
    public boolean glDisableVertexAttribArray(GL2ES2 gl, String name) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        enabledVertexAttribArraySet.remove(name);
        int index = glGetAttribLocation(gl, name);
        if(0>index) {
            if(verbose) {
                Throwable tX = new Throwable("Info: glDisableVertexAttribArray failed, no index for: "+name);
                tX.printStackTrace();
            }
            return false;
        }
        if(DEBUG) {
            System.err.println("Info: glDisableVertexAttribArray: "+name);
        }
        gl.glDisableVertexAttribArray(index);
        return true;
    }

    /**
     * Set the vertex attribute data.
     * Enable the attribute, if it is not enabled yet.
     *
     * Even if the attribute is not found in the current shader,
     * it is stored in this state.
     *
     * @param data the GLArrayData's name must match the attributes one,
     *      it's index will be set with the attribute's location,
     *      if found.
     *
     * @returns false, if the name is not found, otherwise true
     *
     * @throws GLException if the program is not in use
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttributePointer
     * @see #glReleaseAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see #glReplaceShader
     */
    public boolean glVertexAttribPointer(GL2ES2 gl, GLArrayData data) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        if(!enabledVertexAttribArraySet.contains(data.getName())) {
            if(!glEnableVertexAttribArray(gl, data.getName())) {
                if(verbose) {
                    Throwable tX = new Throwable("Info: glVertexAttribPointer: couldn't enable: "+data);
                    tX.printStackTrace();
                }
            }
        }
        int index = getAttribLocation(data.getName());
        if(0>index) {
            if(verbose) {
                Throwable tX = new Throwable("Info: glVertexAttribPointer failed, no index for: "+data);
                tX.printStackTrace();
            }
        }
        data.setLocation(index);
        vertexAttribMap2Data.put(data.getName(), data);
        if(0<=index) {
            // only pass the data, if the attribute exists in the current shader
            if(DEBUG) {
                System.err.println("Info: glVertexAttribPointer: "+data);
            }
            gl.glVertexAttribPointer(data);
            return true;
        }
        return false;
    }

    /**
     * Get the vertex attribute data, previously set.
     *
     * @returns the GLArrayData object, null if not previously set.
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttributePointer
     * @see #glReleaseAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see #glReplaceShader
     */
    public GLArrayData getVertexAttribPointer(String name) {
        return (GLArrayData) vertexAttribMap2Data.get(name);
    }

    /**
     * Releases all mapped vertex attribute data,
     * disables all enabled attributes and loses all indices
     *
     * @throws GLException is the program is not in use but the shaderProgram is set
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttributePointer
     * @see #glReleaseAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see #glReplaceShader
     */
    public void glReleaseAllVertexAttributes(GL2ES2 gl) {
        if(null!=shaderProgram) {
            if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
            for(Iterator iter = vertexAttribMap2Data.keySet().iterator(); iter.hasNext(); ) {
                if(!glDisableVertexAttribArray(gl, (String) iter.next())) {
                    throw new GLException("Internal Error: mapped vertex attribute couldn't be disabled");
                }
            }
            for(Iterator iter = enabledVertexAttribArraySet.iterator(); iter.hasNext(); ) {
                if(!glDisableVertexAttribArray(gl, (String) iter.next())) {
                    throw new GLException("Internal Error: prev enabled vertex attribute couldn't be disabled");
                }
            }
        }
        vertexAttribMap2Data.clear();
        enabledVertexAttribArraySet.clear();
        attribMap2Idx.clear();
    }
        
    /**
     * Disables all vertex attribute arrays.
     *
     * Their enabled stated will be removed from this state only
     * if 'removeFromState' is true.
     *
     * This method purpose is more for debugging. 
     *
     * @throws GLException is the program is not in use but the shaderProgram is set
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttributePointer
     * @see #glReleaseAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see #glReplaceShader
     */
    public void glDisableAllVertexAttributeArrays(GL2ES2 gl, boolean removeFromState) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");

        for(Iterator iter = enabledVertexAttribArraySet.iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            if(removeFromState) {
                enabledVertexAttribArraySet.remove(name);
            }
            int index = glGetAttribLocation(gl, name);
            if(0<=index) {
                gl.glDisableVertexAttribArray(index);
            }
        }
    }

    /**
     * Reset all previously enabled mapped vertex attribute data,
     * incl enabling them
     *
     * @throws GLException is the program is not in use
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttributePointer
     * @see #glReleaseAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see #glReplaceShader
     */
    public void glResetAllVertexAttributes(GL2ES2 gl) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        attribMap2Idx.clear();

        /**
         *
        for(Iterator iter = enabledVertexAttribArraySet.iterator(); iter.hasNext(); ) {
            glEnableVertexAttribArray(gl, (String) iter.next());
        }
        for(Iterator iter = vertexAttribMap2Data.values().iterator(); iter.hasNext(); ) {
            GLArrayData data = (GLArrayData) iter.next();

            ...
        } */

        for(Iterator iter = enabledVertexAttribArraySet.iterator(); iter.hasNext(); ) {
            // get new location ..
            String name = (String) iter.next();
            int loc = glGetAttribLocation(gl, name);

            // get & update data ..
            GLArrayData data = getVertexAttribPointer(name);
            data.setLocation(loc);
            vertexAttribMap2Data.put(name, data);

            if(0>loc) {
                // not used in shader
                continue;
            }

            // enable attrib, VBO and pass location/data
            gl.glEnableVertexAttribArray(loc);

            if( data.isVBO() ) {
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, data.getVBOName());
            } 

            gl.glVertexAttribPointer(data);
        }
    }

    //
    // Shader Uniform handling
    //

    /**
     * Gets the index of a shader uniform.
     * This must be done when the program is in use !
     *
     * @return -1 if there is no such attribute available,
     *         otherwise >= 0

     * @throws GLException is the program is not linked
     *
     * @see #glGetUniformLocation
     * @see javax.media.opengl.GL2ES2#glGetUniformLocation
     * @see #getUniformLocation
     * @see #glReplaceShader
     */
    protected int glGetUniformLocation(GL2ES2 gl, String name) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        int index = getUniformLocation(name);
        if(0>index) {
            index = gl.glGetUniformLocation(shaderProgram.program(), name);
            if(0<=index) {
                Integer idx = new Integer(index);
                uniformMap2Idx.put(name, idx);
            } else if(verbose) {
                Throwable tX = new Throwable("Info: glUniform failed, no location for: "+name+", index: "+index);
                tX.printStackTrace();
            }
        }
        return index;
    }

    protected int getUniformLocation(String name) {
        Integer idx = (Integer) uniformMap2Idx.get(name);
        return (null!=idx)?idx.intValue():-1;
    }

    /**
     * Set the uniform data.
     *
     * Even if the uniform is not found in the current shader,
     * it is stored in this state.
     *
     * @param data the GLUniforms's name must match the uniform one,
     *      it's index will be set with the uniforms's location,
     *      if found.
     *
     *
     * @returns false, if the name is not found, otherwise true
     *
     * @throws GLException if the program is not in use
     *
     * @see #glGetUniformLocation
     * @see javax.media.opengl.GL2ES2#glGetUniformLocation
     * @see #getUniformLocation
     * @see #glReplaceShader
     */
    public boolean glUniform(GL2ES2 gl, GLUniformData data) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        int location = glGetUniformLocation(gl, data.getName());
        data.setLocation(location);
        uniformMap2Data.put(data.getName(), data);
        if(0<=location) {
            // only pass the data, if the uniform exists in the current shader
            if(DEBUG) {
                System.err.println("Info: glUniform: "+data);
            }
            gl.glUniform(data);
        }
        return true;
    }

    /**
     * Get the uniform data, previously set.
     *
     * @returns the GLUniformData object, null if not previously set.
     */
    public GLUniformData getUniform(String name) {
        return (GLUniformData) uniformMap2Data.get(name);
    }

    /**
     * Releases all mapped uniform data
     * and loses all indices
     *
     * @throws GLException is the program is not in use
     */
    public void glReleaseAllUniforms(GL2ES2 gl) {
        uniformMap2Data.clear();
        uniformMap2Idx.clear();
    }
        
    /**
     * Reset all previously mapped uniform data
     *
     * @throws GLException is the program is not in use
     */
    public void glResetAllUniforms(GL2ES2 gl) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        uniformMap2Idx.clear();
        for(Iterator iter = uniformMap2Data.values().iterator(); iter.hasNext(); ) {
            glUniform(gl, (GLUniformData) iter.next());
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("ShaderState[");
        buf.append(shaderProgram.toString());
        buf.append(",EnabledStates: [");
        for(Iterator iter = enabledVertexAttribArraySet.iterator(); iter.hasNext(); ) {
            buf.append("\n  ");
            buf.append((String)iter.next());
        }
        buf.append("], [");
        for(Iterator iter = vertexAttribMap2Data.values().iterator(); iter.hasNext(); ) {
            GLArrayData data = (GLArrayData) iter.next();
            if(data.getLocation()>=0) {
                buf.append("\n  ");
                buf.append(data);
            }
        }
        buf.append("], [");
        for(Iterator iter=uniformMap2Data.values().iterator(); iter.hasNext(); ) {
            GLUniformData data = (GLUniformData) iter.next();
            if(data.getLocation()>=0) {
                buf.append("\n  ");
                buf.append(data);
            }
        }
        buf.append("]");
        return buf.toString();
    }

    protected boolean verbose = false;
    protected ShaderProgram shaderProgram=null;
    protected HashMap attribMap2Idx = new HashMap();
    protected HashSet enabledVertexAttribArraySet = new HashSet();
    protected HashMap vertexAttribMap2Data = new HashMap();
    protected HashMap uniformMap2Idx = new HashMap();
    protected HashMap uniformMap2Data = new HashMap();

}

