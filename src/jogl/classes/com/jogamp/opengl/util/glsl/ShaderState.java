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

package com.jogamp.opengl.util.glsl;

import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLArrayData;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.GLUniformData;

import jogamp.opengl.Debug;

import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.opengl.util.GLArrayDataEditable;

public class ShaderState {
    public static final boolean DEBUG = Debug.isPropertyDefined("jogl.debug.GLSLState", true, AccessController.getContext());

    public ShaderState() {
    }

    public boolean verbose() { return verbose; }

    public void setVerbose(boolean v) { verbose=v; }

    /**
     * Fetches the current shader state from this thread (TLS) current GLContext
     *
     * @see com.jogamp.opengl.util.glsl.ShaderState#glUseProgram(GL2ES2, boolean)
     * @see com.jogamp.opengl.util.glsl.ShaderState#getShaderState(GL)
     * @see com.jogamp.opengl.util.glsl.ShaderState#getCurrentShaderState()
     */
    public static synchronized ShaderState getCurrentShaderState() { 
        return getShaderState(GLContext.getCurrentGL());
    }

    /**
     * Fetches the shader state from the GL object's GLContext
     *
     * @param gl the GL object referencing the GLContext
     * 
     * @see com.jogamp.opengl.util.glsl.ShaderState#glUseProgram(GL2ES2, boolean)
     * @see com.jogamp.opengl.util.glsl.ShaderState#getShaderState(GL)
     * @see com.jogamp.opengl.util.glsl.ShaderState#getCurrentShaderState()
     */
    public static synchronized ShaderState getShaderState(GL gl) { 
        return (ShaderState) gl.getContext().getAttachedObject(ShaderState.class.getName());
    }

    /**
     * Returns the attached user object for the given name to this ShaderState.
     */
    public final Object getAttachedObject(String name) {
      return attachedObjectsByString.get(name);
    }

    /**
     * Attach user object for the given name to this ShaderState.
     * Returns the previously set object or null.
     * 
     * @return the previous mapped object or null if none
     */
    public final Object attachObject(String name, Object obj) {
      return attachedObjectsByString.put(name, obj);
    }

    /**
     * @param name name of the mapped object to detach
     * 
     * @return the previous mapped object or null if none
     */
    public final Object detachObject(String name) {
        return attachedObjectsByString.remove(name);
    }    
    
    /**
     * Returns the attached user object for the given name to this ShaderState.
     */
    public final Object getAttachedObject(int name) {
      return attachedObjectsByInt.get(name);
    }

    /**
     * Attach user object for the given name to this ShaderState.
     * Returns the previously set object or null.
     */
    public final Object attachObject(int name, Object obj) {
      return attachedObjectsByInt.put(name, obj);
    }

    public final Object detachObject(int name) {
        return attachedObjectsByInt.remove(name);
    }    
    
    /**
     * Turns the shader program on or off.<br>
     * Puts this ShaderState to to the thread local storage (TLS),
     * if <code>on</code> is <code>true</code>.
     *
     * @throws GLException if no program is attached
     * @throws GLException if no program is not linked
     *
     * @see com.jogamp.opengl.util.glsl.ShaderState#glUseProgram(GL2ES2, boolean)
     * @see com.jogamp.opengl.util.glsl.ShaderState#getShaderState(GL)
     * @see com.jogamp.opengl.util.glsl.ShaderState#getCurrentShaderState()
     */
    public synchronized void glUseProgram(GL2ES2 gl, boolean on) {
        if(null==shaderProgram) { throw new GLException("No program is attached"); }
        if(on) {
            shaderProgram.useProgram(gl, true);
            // update the current ShaderState to the TLS ..
            gl.getContext().attachObject(ShaderState.class.getName(), this);
            if(resetAllShaderData) {
                resetAllShaderData = false;
                glResetAllVertexAttributes(gl);
                glResetAllUniforms(gl);
            }
        } else {
            shaderProgram.useProgram(gl, false);
        }
    }

    public synchronized void glUseProgram(GL2ES2 gl, ShaderProgram prog, boolean on) {
        
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
            glUseProgram(gl, false);
            resetAllShaderData = true;
        }

        // register new one
        shaderProgram = prog;

        if(null!=shaderProgram) {
            // reinstall all data ..
            if(shaderProgram.linked()) {
                glUseProgram(gl, true);
                if(!prgInUse) {
                    shaderProgram.useProgram(gl, false);
                }
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
        attachedObjectsByString.clear();        
        attachedObjectsByInt.clear();
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
                shaderProgram.useProgram(gl, true);
            }
        }
        glReleaseAllVertexAttributes(gl);
        glReleaseAllUniforms(gl);
        if(null!=shaderProgram) {
            if(releaseProgramToo) {
                shaderProgram.release(gl, releaseShaderToo);
            } else if(!prgInUse) {
                shaderProgram.useProgram(gl, false);
            }
        }
    }

    //
    // Shader attribute handling
    //

    /**
     * Gets the cached location of a shader attribute.
     *
     * @return -1 if there is no such attribute available, 
     *         otherwise >= 0
     *
     * @see #glBindAttribLocation(GL2ES2, int, String)
     * @see #glBindAttribLocation(GL2ES2, int, GLArrayData)
     * @see #glGetAttribLocation(GL2ES2, String)
     * @see GL2ES2#glGetAttribLocation(int, String)
     */
    public int getAttribLocation(String name) {
        Integer idx = (Integer) attribMap2Location.get(name);
        return (null!=idx)?idx.intValue():-1;
    }
    
    /**
     * Get the previous cached vertex attribute data.
     *
     * @return the GLArrayData object, null if not previously set.
     *
     * @see #bindAttribute(GLArrayData)
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     * @see #glReleaseAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see ShaderProgram#glReplaceShader
     */
    public GLArrayData getAttribute(String name) {
        return (GLArrayData) vertexAttribMap2Data.get(name);
    }
    
    /**
     * Bind the {@link GLArrayData} to this ShaderState. 
     * If an attribute location is cached (ie {@link #glBindAttribLocation(GL2ES2, int, String)})
     * it is promoted to the {@link GLArrayData} instance.
     * To bind a {@link GLArrayData} with a given location
     * {@link #glBindAttribLocation(GL2ES2, int, GLArrayData)} shall be used.
     * 
     * @param data the {@link GLArrayData} to be processed
     * @see #glBindAttribLocation(GL2ES2, int, String)
     * @see #getAttribute(String)
     */
    public void bindAttribute(GLArrayData data) {
        final int location = getAttribLocation(data.getName());
        if(0<=location) {
            data.setLocation(location);
        }
        vertexAttribMap2Data.put(data.getName(), data);
    }
    
    /**
     * Binds a shader attribute to a location.
     * Multiple names can be bound to one location.
     * The value will be cached and can be retrieved via {@link #getAttribLocation(String)}
     * before or after linking.
     *
     * @throws GLException if no program is attached
     * @throws GLException if the program is already linked
     * 
     * @see javax.media.opengl.GL2ES2#glBindAttribLocation(int, int, String)
     * @see #glGetAttribLocation(GL2ES2, String)
     * @see #getAttribLocation(String)
     */
    public void glBindAttribLocation(GL2ES2 gl, int location, String name) {
        if(null==shaderProgram) throw new GLException("No program is attached");
        if(shaderProgram.linked()) throw new GLException("Program is already linked");        
        final Integer loc = new Integer(location);
        attribMap2Location.put(name, loc);
        gl.glBindAttribLocation(shaderProgram.program(), location, name);
    }

    /**
     * Binds a shader {@link GLArrayData} attribute to a location.
     * Multiple names can be bound to one location.
     * The value will be cached and can be retrieved via {@link #getAttribLocation(String)}
     * and {@link #getAttribute(String)}before or after linking.
     * The {@link GLArrayData}'s location will be set as well.
     *
     * @throws GLException if no program is attached
     * @throws GLException if the program is already linked
     * 
     * @see javax.media.opengl.GL2ES2#glBindAttribLocation(int, int, String)
     * @see #glGetAttribLocation(GL2ES2, String)
     * @see #getAttribLocation(String)
     * @see #getAttribute(String)
     */
    public void glBindAttribLocation(GL2ES2 gl, int location, GLArrayData data) {
        glBindAttribLocation(gl, location, data.getName());
        data.setLocation(location);
        vertexAttribMap2Data.put(data.getName(), data);
    }

    /**
     * Gets the location of a shader attribute,
     * either the cached value {@link #getAttribLocation(String)} if valid or
     * the retrieved one {@link GL2ES2#glGetAttribLocation(int, String)}.
     * In the latter case the value will be cached.
     *
     * @return -1 if there is no such attribute available, 
     *         otherwise >= 0
     * @throws GLException if no program is attached
     * @throws GLException if the program is not linked and no location was cached.
     *
     * @see #getAttribLocation(String)
     * @see #glBindAttribLocation(GL2ES2, int, GLArrayData)
     * @see #glBindAttribLocation(GL2ES2, int, String)
     * @see GL2ES2#glGetAttribLocation(int, String)
     */
    public int glGetAttribLocation(GL2ES2 gl, String name) {
        if(null==shaderProgram) throw new GLException("No program is attached");
        int location = getAttribLocation(name);
        if(0>location) {
            if(!shaderProgram.linked()) throw new GLException("Program is not linked");
            location = gl.glGetAttribLocation(shaderProgram.program(), name);
            if(0<=location) {
                Integer idx = new Integer(location);
                attribMap2Location.put(name, idx);
                if(DEBUG) {
                    System.err.println("Info: glGetAttribLocation: "+name+", loc: "+location);
                }
            } else if(verbose) {
                Throwable tX = new Throwable("Info: glGetAttribLocation failed, no location for: "+name+", loc: "+location);
                tX.printStackTrace();
            }
        }
        return location;
    }

    /**
     * Gets the location of a shader attribute,
     * either the cached value {@link #getAttribLocation(String)} if valid or
     * the retrieved one {@link GL2ES2#glGetAttribLocation(int, String)}.
     * In the latter case the value will be cached.
     * The {@link GLArrayData}'s location will be set as well.
     *
     * @return -1 if there is no such attribute available, 
     *         otherwise >= 0
     *         
     * @throws GLException if no program is attached
     * @throws GLException if the program is not linked and no location was cached.
     *
     * @see #getAttribLocation(String)
     * @see #glBindAttribLocation(GL2ES2, int, GLArrayData)
     * @see #glBindAttribLocation(GL2ES2, int, String)
     * @see GL2ES2#glGetAttribLocation(int, String)
     * @see #getAttribute(String)
     */
    public int glGetAttribLocation(GL2ES2 gl, GLArrayData data) {
        int location = glGetAttribLocation(gl, data.getName());
        data.setLocation(location);
        vertexAttribMap2Data.put(data.getName(), data);
        return location;
    }
    
    //
    // Enabled Vertex Arrays and its data
    //

    /**
     * @return true if the named attribute is enable
     */
    public final boolean isVertexAttribArrayEnabled(String name) {
        return enabledVertexAttribArraySet.contains(name);
    }
    
    /**
     * @return true if the {@link GLArrayData} attribute is enable
     */
    public final boolean isVertexAttribArrayEnabled(GLArrayData data) {
        return isVertexAttribArrayEnabled(data.getName());
    }
    
    private boolean glEnableVertexAttribArray(GL2ES2 gl, String name, int location) {
        enabledVertexAttribArraySet.add(name);
        if(0>location) {
            location = glGetAttribLocation(gl, name);
            if(0>location) {
                if(verbose) {
                    Throwable tX = new Throwable("Info: glEnableVertexAttribArray failed, no index for: "+name);
                    tX.printStackTrace();
                }
                return false;
            }
        }
        if(DEBUG) {
            System.err.println("Info: glEnableVertexAttribArray: "+name+", loc: "+location);
        }
        gl.glEnableVertexAttribArray(location);
        return true;
    }
    
    /**
     * Enables a vertex attribute array.
     * 
     * This method retrieves the the location via {@link #glGetAttribLocation(GL2ES2, GLArrayData)}
     * hence {@link #glEnableVertexAttribArray(GL2ES2, GLArrayData)} shall be preferred. 
     *
     * Even if the attribute is not found in the current shader,
     * it is marked enabled in this state.
     *
     * @return false, if the name is not found, otherwise true
     *
     * @throws GLException if no program is attached
     * @throws GLException if the program is not linked and no location was cached.
     * 
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     */
    public boolean glEnableVertexAttribArray(GL2ES2 gl, String name) {
        return glEnableVertexAttribArray(gl, name, -1);
    }
    

    /**
     * Enables a vertex attribute array, usually invoked by {@link GLArrayDataEditable#enableBuffer(GL, boolean)}.
     *
     * This method uses the {@link GLArrayData}'s location if set
     * and is the preferred alternative to {@link #glEnableVertexAttribArray(GL2ES2, String)}.
     * If data location is unset it will be retrieved via {@link #glGetAttribLocation(GL2ES2, GLArrayData)} set
     * and cached in this state.
     *  
     * Even if the attribute is not found in the current shader,
     * it is marked enabled in this state.
     *
     * @return false, if the name is not found, otherwise true
     *
     * @throws GLException if no program is attached
     * @throws GLException if the program is not linked and no location was cached.
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     * @see GLArrayDataEditable#enableBuffer(GL, boolean)
     */
    public boolean glEnableVertexAttribArray(GL2ES2 gl, GLArrayData data) {
        if(null==shaderProgram) throw new GLException("No program is attached");
        if(0 > data.getLocation()) {
            glGetAttribLocation(gl, data);
        } else {
            // ensure data is the current bound one
            vertexAttribMap2Data.put(data.getName(), data);             
        }
        return glEnableVertexAttribArray(gl, data.getName(), data.getLocation());
    }
    
    private boolean glDisableVertexAttribArray(GL2ES2 gl, String name, int location) {
        enabledVertexAttribArraySet.remove(name);
        if(0>location) {
            location = glGetAttribLocation(gl, name);
            if(0>location) {
                if(verbose) {
                    Throwable tX = new Throwable("Info: glDisableVertexAttribArray failed, no index for: "+name);
                    tX.printStackTrace();
                }
                return false;
            }
        }
        if(DEBUG) {
            System.err.println("Info: glDisableVertexAttribArray: "+name);
        }
        gl.glDisableVertexAttribArray(location);
        return true;
    }
    
    /**
     * Disables a vertex attribute array
     *
     * This method retrieves the the location via {@link #glGetAttribLocation(GL2ES2, GLArrayData)}
     * hence {@link #glDisableVertexAttribArray(GL2ES2, GLArrayData)} shall be preferred.
     *  
     * Even if the attribute is not found in the current shader,
     * it is removed from this state enabled list.
     *
     * @return false, if the name is not found, otherwise true
     *
     * @throws GLException if no program is attached
     * @throws GLException if the program is not linked and no location was cached.
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     */
    public boolean glDisableVertexAttribArray(GL2ES2 gl, String name) {
        return glDisableVertexAttribArray(gl, name, -1);
    }

    /**
     * Disables a vertex attribute array
     *
     * This method uses the {@link GLArrayData}'s location if set
     * and is the preferred alternative to {@link #glDisableVertexAttribArray(GL2ES2, String)}.
     * If data location is unset it will be retrieved via {@link #glGetAttribLocation(GL2ES2, GLArrayData)} set
     * and cached in this state.
     *  
     * Even if the attribute is not found in the current shader,
     * it is removed from this state enabled list.
     *
     * @return false, if the name is not found, otherwise true
     *
     * @throws GLException if no program is attached
     * @throws GLException if the program is not linked and no location was cached.
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     */
    public boolean glDisableVertexAttribArray(GL2ES2 gl, GLArrayData data) {
        if(null==shaderProgram) throw new GLException("No program is attached");
        if(0 > data.getLocation()) {
            glGetAttribLocation(gl, data);
        }
        return glDisableVertexAttribArray(gl, data.getName(), data.getLocation());
    }
    
    /**
     * Set the {@link GLArrayData} vertex attribute data.
     * 
     * This method uses the {@link GLArrayData}'s location if set.
     * If data location is unset it will be retrieved via {@link #glGetAttribLocation(GL2ES2, GLArrayData)}, set
     * and cached in this state.
     * 
     * @return false, if the location could not be determined, otherwise true
     *
     * @throws GLException if the program is not in use
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     */
    public boolean glVertexAttribPointer(GL2ES2 gl, GLArrayData data) {
        if(null==shaderProgram) throw new GLException("No program is attached");
        // if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        if(0 > data.getLocation()) {
            glGetAttribLocation(gl, data);
        } /* else {
            // Already achieved by glEnableVertexAttribArray(..)
            // ensure data is the current bound one
            vertexAttribMap2Data.put(data.getName(), data); 
        } */
        if(0 <= data.getLocation()) {
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
     * Releases all mapped vertex attribute data,
     * disables all enabled attributes and loses all indices
     *
     * @throws GLException is the program is not in use but the shaderProgram is set
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     * @see #glReleaseAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see ShaderProgram#glReplaceShader
     */
    public void glReleaseAllVertexAttributes(GL2ES2 gl) {
        if(null!=shaderProgram) {
            if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
            for(Iterator<GLArrayData> iter = vertexAttribMap2Data.values().iterator(); iter.hasNext(); ) {
                if(!glDisableVertexAttribArray(gl, iter.next())) {
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
        attribMap2Location.clear();
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
     * @see #getVertexAttribPointer
     * @see #glReleaseAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see ShaderProgram#glReplaceShader
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
     * Reset all previously enabled mapped vertex attribute data.
     * 
     * Attribute data is bound to the GL state<br>
     * Attribute location is bound to the program<br>
     * 
     * However, since binding an attribute to a location via {@link #glBindAttribLocation(GL2ES2, int, GLArrayData)}
     * <i>must</i> happen before linking <b>and</b> we try to promote the attributes to the new program,
     * we have to gather the probably new location etc.
     *
     * @throws GLException is the program is not in use
     *
     * @see #attachShaderProgram(GL2ES2, ShaderProgram)
     */
    public void glResetAllVertexAttributes(GL2ES2 gl) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        attribMap2Location.clear();
        
        for(Iterator<GLArrayData> iter = vertexAttribMap2Data.values().iterator(); iter.hasNext(); ) {        
            GLArrayData data = iter.next();
            
            // get new location ..
            String name = data.getName();
            int loc = glGetAttribLocation(gl, name);
            data.setLocation(loc);

            if(0>loc) {
                // not used in shader
                continue;
            }
            
            if(enabledVertexAttribArraySet.contains(name)) {
                // enable attrib, VBO and pass location/data
                gl.glEnableVertexAttribArray(loc);
            }

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
     * @see ShaderProgram#glReplaceShader
     */
    protected int glGetUniformLocation(GL2ES2 gl, String name) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        int location = getUniformLocation(name);
        if(0>location) {
            location = gl.glGetUniformLocation(shaderProgram.program(), name);
            if(0<=location) {
                Integer idx = new Integer(location);
                uniformMap2Location.put(name, idx);
            } else if(verbose) {
                Throwable tX = new Throwable("Info: glUniform failed, no location for: "+name+", index: "+location);
                tX.printStackTrace();
            }
        }
        return location;
    }

    protected int getUniformLocation(String name) {
        Integer idx = (Integer) uniformMap2Location.get(name);
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
     * @return false, if the name is not found, otherwise true
     *
     * @throws GLException if the program is not in use
     *
     * @see #glGetUniformLocation
     * @see javax.media.opengl.GL2ES2#glGetUniformLocation
     * @see #getUniformLocation
     * @see ShaderProgram#glReplaceShader
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
     * @return the GLUniformData object, null if not previously set.
     */
    public GLUniformData getUniform(String name) {
        return uniformMap2Data.get(name);
    }

    /**
     * Releases all mapped uniform data
     * and loses all indices
     *
     * @throws GLException is the program is not in use
     */
    public void glReleaseAllUniforms(GL2ES2 gl) {
        uniformMap2Data.clear();
        uniformMap2Location.clear();
    }
        
    /**
     * Reset all previously mapped uniform data
     * 
     * Uniform data and location is bound to the program,
     * hence both are updated here
     *
     * @throws GLException is the program is not in use
     * 
     * @see #attachShaderProgram(GL2ES2, ShaderProgram)
     */
    public void glResetAllUniforms(GL2ES2 gl) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        uniformMap2Location.clear();
        for(Iterator<GLUniformData> iter = uniformMap2Data.values().iterator(); iter.hasNext(); ) {
            glUniform(gl, iter.next());
        }
    }

    public StringBuilder toString(StringBuilder sb) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        
        sb.append("ShaderState[");
        sb.append(shaderProgram.toString());
        sb.append(",EnabledStates: [");
        for(Iterator iter = enabledVertexAttribArraySet.iterator(); iter.hasNext(); ) {
            sb.append("\n  ");
            sb.append((String)iter.next());
        }
        sb.append("], [");
        for(Iterator<GLArrayData> iter = vertexAttribMap2Data.values().iterator(); iter.hasNext(); ) {
            GLArrayData data = iter.next();
            if(data.getLocation()>=0) {
                sb.append("\n  ");
                sb.append(data);
            }
        }
        sb.append("], [");
        for(Iterator<GLUniformData> iter=uniformMap2Data.values().iterator(); iter.hasNext(); ) {
            GLUniformData data = iter.next();
            if(data.getLocation()>=0) {
                sb.append("\n  ");
                sb.append(data);
            }
        }
        sb.append("]");
        return sb;
    }
    
    @Override
    public String toString() {
        return toString(null).toString();
    }
    
    protected boolean verbose = false;
    protected ShaderProgram shaderProgram=null;
    
    protected HashSet<String> enabledVertexAttribArraySet = new HashSet<String>();
    protected HashMap<String, Integer> attribMap2Location = new HashMap<String, Integer>();
    protected HashMap<String, GLArrayData> vertexAttribMap2Data = new HashMap<String, GLArrayData>();
    
    protected HashMap<String, Integer> uniformMap2Location = new HashMap<String, Integer>();
    protected HashMap<String, GLUniformData> uniformMap2Data = new HashMap<String, GLUniformData>();
    
    private HashMap<String, Object> attachedObjectsByString = new HashMap<String, Object>();    
    private IntObjectHashMap attachedObjectsByInt = new IntObjectHashMap();   
    private boolean resetAllShaderData = false;
}

