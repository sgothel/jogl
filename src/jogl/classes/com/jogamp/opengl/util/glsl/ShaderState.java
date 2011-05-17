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
import java.util.ArrayList;
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

import com.jogamp.common.os.Platform;
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
     * @see com.jogamp.opengl.util.glsl.ShaderState#useProgram(GL2ES2, boolean)
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
     * @see com.jogamp.opengl.util.glsl.ShaderState#useProgram(GL2ES2, boolean)
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
     *
     * @see com.jogamp.opengl.util.glsl.ShaderState#useProgram(GL2ES2, boolean)
     * @see com.jogamp.opengl.util.glsl.ShaderState#getShaderState(GL)
     * @see com.jogamp.opengl.util.glsl.ShaderState#getCurrentShaderState()
     */
    public synchronized void useProgram(GL2ES2 gl, boolean on) throws GLException {
        if(null==shaderProgram) { throw new GLException("No program is attached"); }        
        if(on) {
            // update the current ShaderState to the TLS ..
            gl.getContext().attachObject(ShaderState.class.getName(), this);
            if(shaderProgram.linked()) {
                shaderProgram.useProgram(gl, true);
                if(resetAllShaderData) {
                    resetAllAttributes(gl);
                    resetAllUniforms(gl);
                }
            } else { 
                if(resetAllShaderData) {
                    setAllAttributes(gl);
                }
                if(!shaderProgram.link(gl, System.err)) {
                    throw new GLException("could not link program: "+shaderProgram);
                }
                shaderProgram.useProgram(gl, true);
                if(resetAllShaderData) {
                    resetAllUniforms(gl);
                }
            }
            resetAllShaderData = false;            
        } else {
            shaderProgram.useProgram(gl, false);
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
     * <p>Attaching a shader program the first time, 
     * as well as switching to another program on the fly,
     * while managing all attribute and uniform data.</p>
     * 
     * <p>[Re]sets all data and use program in case of a program switch.<br> 
     * Use program if linked in case of a 1st time attachment.</p>
     * 
     * @throws GLException if program was not linked and linking fails
     */
    public synchronized void attachShaderProgram(GL2ES2 gl, ShaderProgram prog) throws GLException {
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
            
            if(prgInUse) {
                // only disable if in use
                if(null != prog) {
                    // new program will issue glUseProgram(..)
                    shaderProgram.programInUse = false;
                } else {
                    // no new program - disable
                    useProgram(gl, false);
                }
            }
            resetAllShaderData = true;
        }

        // register new one
        shaderProgram = prog;

        if(null!=shaderProgram) {
            // [re]set all data and use program if switching program, 
            // or  use program if program is linked
            if(shaderProgram.linked() || resetAllShaderData) {
                useProgram(gl, true); // may reset all data
                if(!prgInUse) {
                    useProgram(gl, false);
                }
            }
        }
        if(DEBUG) {
            System.err.println("Info: attachShaderProgram: END");
        }
    }

    public ShaderProgram shaderProgram() { return shaderProgram; }

    /**
     * Calls {@link #release(GL2ES2, boolean, boolean, boolean) release(gl, true, true, true)}
     *
     * @see #glReleaseAllVertexAttributes
     * @see #glReleaseAllUniforms
     * @see #release(GL2ES2, boolean, boolean, boolean)
     */
    public synchronized void destroy(GL2ES2 gl) {
        release(gl, true, true, true);
        attachedObjectsByString.clear();        
        attachedObjectsByInt.clear();
    }

    /**
     * Calls {@link #release(GL2ES2, boolean, boolean, boolean) release(gl, false, false, false)}
     *
     * @see #glReleaseAllVertexAttributes
     * @see #glReleaseAllUniforms
     * @see #release(GL2ES2, boolean, boolean, boolean)
     */
    public synchronized void releaseAllData(GL2ES2 gl) {
        release(gl, false, false, false);
    }

    /**
     * @see #glReleaseAllVertexAttributes
     * @see #glReleaseAllUniforms
     * @see ShaderProgram#release(GL2ES2, boolean)
     */
    public synchronized void release(GL2ES2 gl, boolean destroyBoundAttributes, boolean releaseProgramToo, boolean releaseShaderToo) {
        if(null!=shaderProgram) {            
            shaderProgram.useProgram(gl, false);
        }
        if(destroyBoundAttributes) {
            for(Iterator<GLArrayData> iter = managedAttributes.iterator(); iter.hasNext(); ) {
                iter.next().destroy(gl);
            }            
        }
        releaseAllAttributes(gl);
        releaseAllUniforms(gl);
        if(null!=shaderProgram) {
            if(releaseProgramToo) {
                shaderProgram.release(gl, releaseShaderToo);
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
     * @see #bindAttribLocation(GL2ES2, int, String)
     * @see #bindAttribLocation(GL2ES2, int, GLArrayData)
     * @see #getAttribLocation(GL2ES2, String)
     * @see GL2ES2#glGetAttribLocation(int, String)
     */
    public int getAttribLocation(String name) {
        Integer idx = (Integer) activeAttribLocationMap.get(name);
        return (null!=idx)?idx.intValue():-1;
    }
    
    /**
     * Get the previous cached vertex attribute data.
     *
     * @return the GLArrayData object, null if not previously set.
     *
     * @see #ownAttribute(GLArrayData, boolean)
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
        return (GLArrayData) activeAttribDataMap.get(name);
    }
    
    /**
     * Binds or unbinds the {@link GLArrayData} lifecycle to this ShaderState.
     *  
     * <p>If an attribute location is cached (ie {@link #bindAttribLocation(GL2ES2, int, String)})
     * it is promoted to the {@link GLArrayData} instance.</p>
     * 
     * <p>The attribute will be destroyed with {@link #destroy(GL2ES2)} 
     * and it's location will be reset when switching shader with {@link #attachShaderProgram(GL2ES2, ShaderProgram)}.</p>
     *  
     * <p>The data will not be transfered to the GPU, use {@link #vertexAttribPointer(GL2ES2, GLArrayData)} additionally.</p>
     * 
     * @param attribute the {@link GLArrayData} which lifecycle shall be managed
     * @param own true if <i>owning</i> shall be performs, false if <i>disowning</i>.
     * 
     * @see #bindAttribLocation(GL2ES2, int, String)
     * @see #getAttribute(String)
     */
    public void ownAttribute(GLArrayData attribute, boolean own) {
        if(own) {
            final int location = getAttribLocation(attribute.getName());
            if(0<=location) {
                attribute.setLocation(location);
            }
            managedAttributes.add(managedAttributes.size(), attribute);
        } else {
            managedAttributes.remove(attribute);
        }
    }
    
    public boolean ownsAttribute(GLArrayData attribute) {
        return managedAttributes.contains(attribute);
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
     * @see #getAttribLocation(GL2ES2, String)
     * @see #getAttribLocation(String)
     */
    public void bindAttribLocation(GL2ES2 gl, int location, String name) {
        if(null==shaderProgram) throw new GLException("No program is attached");
        if(shaderProgram.linked()) throw new GLException("Program is already linked");        
        final Integer loc = new Integer(location);
        activeAttribLocationMap.put(name, loc);
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
     * @see #getAttribLocation(GL2ES2, String)
     * @see #getAttribLocation(String)
     * @see #getAttribute(String)
     */
    public void bindAttribLocation(GL2ES2 gl, int location, GLArrayData data) {
        bindAttribLocation(gl, location, data.getName());
        data.setLocation(location);
        activeAttribDataMap.put(data.getName(), data);
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
     * @see #bindAttribLocation(GL2ES2, int, GLArrayData)
     * @see #bindAttribLocation(GL2ES2, int, String)
     * @see GL2ES2#glGetAttribLocation(int, String)
     */
    public int getAttribLocation(GL2ES2 gl, String name) {
        if(null==shaderProgram) throw new GLException("No program is attached");
        int location = getAttribLocation(name);
        if(0>location) {
            if(!shaderProgram.linked()) throw new GLException("Program is not linked");
            location = gl.glGetAttribLocation(shaderProgram.program(), name);
            if(0<=location) {
                Integer idx = new Integer(location);
                activeAttribLocationMap.put(name, idx);
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
     * @see #bindAttribLocation(GL2ES2, int, GLArrayData)
     * @see #bindAttribLocation(GL2ES2, int, String)
     * @see GL2ES2#glGetAttribLocation(int, String)
     * @see #getAttribute(String)
     */
    public int getAttribLocation(GL2ES2 gl, GLArrayData data) {
        int location = getAttribLocation(gl, data.getName());
        data.setLocation(location);
        activeAttribDataMap.put(data.getName(), data);
        return location;
    }
    
    //
    // Enabled Vertex Arrays and its data
    //

    /**
     * @return true if the named attribute is enable
     */
    public final boolean isVertexAttribArrayEnabled(String name) {
        return enabledAttributes.contains(name);
    }
    
    /**
     * @return true if the {@link GLArrayData} attribute is enable
     */
    public final boolean isVertexAttribArrayEnabled(GLArrayData data) {
        return isVertexAttribArrayEnabled(data.getName());
    }
    
    private boolean enableVertexAttribArray(GL2ES2 gl, String name, int location) {
        enabledAttributes.add(name);
        if(0>location) {
            location = getAttribLocation(gl, name);
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
     * This method retrieves the the location via {@link #getAttribLocation(GL2ES2, GLArrayData)}
     * hence {@link #enableVertexAttribArray(GL2ES2, GLArrayData)} shall be preferred. 
     *
     * Even if the attribute is not found in the current shader,
     * it is marked enabled in this state.
     *
     * @return false, if the name is not found, otherwise true
     *
     * @throws GLException if the program is not linked and no location was cached.
     * 
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     */
    public boolean enableVertexAttribArray(GL2ES2 gl, String name) {
        return enableVertexAttribArray(gl, name, -1);
    }
    

    /**
     * Enables a vertex attribute array, usually invoked by {@link GLArrayDataEditable#enableBuffer(GL, boolean)}.
     *
     * This method uses the {@link GLArrayData}'s location if set
     * and is the preferred alternative to {@link #enableVertexAttribArray(GL2ES2, String)}.
     * If data location is unset it will be retrieved via {@link #getAttribLocation(GL2ES2, GLArrayData)} set
     * and cached in this state.
     *  
     * Even if the attribute is not found in the current shader,
     * it is marked enabled in this state.
     *
     * @return false, if the name is not found, otherwise true
     *
     * @throws GLException if the program is not linked and no location was cached.
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     * @see GLArrayDataEditable#enableBuffer(GL, boolean)
     */
    public boolean enableVertexAttribArray(GL2ES2 gl, GLArrayData data) {
        if(0 > data.getLocation()) {
            getAttribLocation(gl, data);
        } else {
            // ensure data is the current bound one
            activeAttribDataMap.put(data.getName(), data);             
        }
        return enableVertexAttribArray(gl, data.getName(), data.getLocation());
    }
    
    private boolean disableVertexAttribArray(GL2ES2 gl, String name, int location) {
        enabledAttributes.remove(name);
        if(0>location) {
            location = getAttribLocation(gl, name);
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
     * This method retrieves the the location via {@link #getAttribLocation(GL2ES2, GLArrayData)}
     * hence {@link #disableVertexAttribArray(GL2ES2, GLArrayData)} shall be preferred.
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
    public boolean disableVertexAttribArray(GL2ES2 gl, String name) {
        return disableVertexAttribArray(gl, name, -1);
    }

    /**
     * Disables a vertex attribute array
     *
     * This method uses the {@link GLArrayData}'s location if set
     * and is the preferred alternative to {@link #disableVertexAttribArray(GL2ES2, String)}.
     * If data location is unset it will be retrieved via {@link #getAttribLocation(GL2ES2, GLArrayData)} set
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
    public boolean disableVertexAttribArray(GL2ES2 gl, GLArrayData data) {
        if(0 > data.getLocation()) {
            getAttribLocation(gl, data);
        }
        return disableVertexAttribArray(gl, data.getName(), data.getLocation());
    }
    
    /**
     * Set the {@link GLArrayData} vertex attribute data.
     * 
     * This method uses the {@link GLArrayData}'s location if set.
     * If data location is unset it will be retrieved via {@link #getAttribLocation(GL2ES2, GLArrayData)}, set
     * and cached in this state.
     * 
     * @return false, if the location could not be determined, otherwise true
     *
     * @throws GLException if no program is attached
     * @throws GLException if the program is not linked and no location was cached.
     * 
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     */
    public boolean vertexAttribPointer(GL2ES2 gl, GLArrayData data) {
        int location = data.getLocation();
        if(0 > location) {
            location = getAttribLocation(gl, data);
        } /* else { 
            done via enable ..
            // ensure data is the current bound one
            activeAttribDataMap.put(data.getName(), data);             
        } */
        if(0 <= location) {
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
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     * @see #glReleaseAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see ShaderProgram#glReplaceShader
     */
    public void releaseAllAttributes(GL2ES2 gl) {
        if(null!=shaderProgram) {
            for(Iterator<GLArrayData> iter = activeAttribDataMap.values().iterator(); iter.hasNext(); ) {
                if(!disableVertexAttribArray(gl, iter.next())) {
                    throw new GLException("Internal Error: mapped vertex attribute couldn't be disabled");
                }
            }
            for(Iterator<String> iter = enabledAttributes.iterator(); iter.hasNext(); ) {
                if(!disableVertexAttribArray(gl, iter.next())) {
                    throw new GLException("Internal Error: prev enabled vertex attribute couldn't be disabled");
                }
            }
        }
        activeAttribDataMap.clear();
        enabledAttributes.clear();
        activeAttribLocationMap.clear();
        managedAttributes.clear();        
    }
        
    /**
     * Disables all vertex attribute arrays.
     *
     * Their enabled stated will be removed from this state only
     * if 'removeFromState' is true.
     *
     * This method purpose is more for debugging. 
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
    public void disableAllVertexAttributeArrays(GL2ES2 gl, boolean removeFromState) {
        for(Iterator<String> iter = enabledAttributes.iterator(); iter.hasNext(); ) {
            final String name = iter.next();
            if(removeFromState) {
                enabledAttributes.remove(name);
            }
            final int index = getAttribLocation(gl, name);
            if(0<=index) {
                gl.glDisableVertexAttribArray(index);
            }
        }
    }

    private final void relocateAttribute(GL2ES2 gl, GLArrayData attribute) {
        // get new location ..
        final String name = attribute.getName();
        final int loc = getAttribLocation(gl, name);
        attribute.setLocation(loc);

        if(0<=loc) {
            if(enabledAttributes.contains(name)) {
                // enable attrib, VBO and pass location/data
                gl.glEnableVertexAttribArray(loc);
            }
    
            if( attribute.isVBO() ) {
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, attribute.getVBOName());
            } 
    
            gl.glVertexAttribPointer(attribute);
        }
    }
    
    /**
     * Reset all previously enabled mapped vertex attribute data.
     * 
     * <p>Attribute data is bound to the GL state</p>
     * <p>Attribute location is bound to the program</p>
     * 
     * <p>However, since binding an attribute to a location via {@link #bindAttribLocation(GL2ES2, int, GLArrayData)}
     * <i>must</i> happen before linking <b>and</b> we try to promote the attributes to the new program,
     * we have to gather the probably new location etc.</p>
     *
     * @throws GLException is the program is not linked
     *
     * @see #attachShaderProgram(GL2ES2, ShaderProgram)
     */
    private final void resetAllAttributes(GL2ES2 gl) {
        if(!shaderProgram.linked()) throw new GLException("Program is not linked");
        activeAttribLocationMap.clear();
        
        for(Iterator<GLArrayData> iter = managedAttributes.iterator(); iter.hasNext(); ) {
            iter.next().setLocation(-1);
        }
        for(Iterator<GLArrayData> iter = activeAttribDataMap.values().iterator(); iter.hasNext(); ) {
            relocateAttribute(gl, iter.next());
        }
    }

    private final void setAttribute(GL2ES2 gl, GLArrayData attribute) {
        // get new location ..
        final String name = attribute.getName();
        final int loc = attribute.getLocation();

        if(0<=loc) {
            this.bindAttribLocation(gl, loc, name);
            
            if(enabledAttributes.contains(name)) {
                // enable attrib, VBO and pass location/data
                gl.glEnableVertexAttribArray(loc);
            }
    
            if( attribute.isVBO() ) {
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, attribute.getVBOName());
            } 
    
            gl.glVertexAttribPointer(attribute);
        }
    }
    
    /**
     * preserves the attribute location .. (program not linked)
     */
    private final void setAllAttributes(GL2ES2 gl) {
        for(Iterator<GLArrayData> iter = activeAttribDataMap.values().iterator(); iter.hasNext(); ) {
            setAttribute(gl, iter.next());
        }
    }

    //
    // Shader Uniform handling
    //

    /**
     * Bind the {@link GLUniform} lifecycle to this ShaderState.
     *  
     * <p>If a uniform location is cached it is promoted to the {@link GLUniformData} instance.</p>
     * 
     * <p>The attribute will be destroyed with {@link #destroy(GL2ES2)} 
     * and it's location will be reset when switching shader with {@link #attachShaderProgram(GL2ES2, ShaderProgram)}.</p>
     *  
     * <p>The data will not be transfered to the GPU, use {@link #uniform(GL2ES2, GLUniformData)} additionally.</p>
     * 
     * @param uniform the {@link GLUniformData} which lifecycle shall be managed
     * 
     * @see #getUniform(String)
     */
    public void ownUniform(GLUniformData uniform) {
        final int location = getUniformLocation(uniform.getName());
        if(0<=location) {
            uniform.setLocation(location);
        }        
        activeUniformDataMap.put(uniform.getName(), uniform);
        managedUniforms.add(uniform);        
    }
    
    public boolean ownsUniform(GLUniformData uniform) {
        return managedUniforms.contains(uniform);
    }
    
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
    protected final int getUniformLocation(GL2ES2 gl, String name) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        int location = getUniformLocation(name);
        if(0>location) {
            location = gl.glGetUniformLocation(shaderProgram.program(), name);
            if(0<=location) {
                Integer idx = new Integer(location);
                activeUniformLocationMap.put(name, idx);
            } else if(verbose) {
                Throwable tX = new Throwable("Info: glUniform failed, no location for: "+name+", index: "+location);
                tX.printStackTrace();
            }
        }
        return location;
    }

    protected final int getUniformLocation(String name) {
        Integer idx = (Integer) activeUniformLocationMap.get(name);
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
    public boolean uniform(GL2ES2 gl, GLUniformData data) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        int location = data.getLocation();
        if(0>location) {
            location = getUniformLocation(gl, data.getName());
            data.setLocation(location);
        }
        activeUniformDataMap.put(data.getName(), data);
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
        return activeUniformDataMap.get(name);
    }

    /**
     * Releases all mapped uniform data
     * and loses all indices
     */
    public void releaseAllUniforms(GL2ES2 gl) {
        activeUniformDataMap.clear();
        activeUniformLocationMap.clear();
        managedUniforms.clear();
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
    private final void resetAllUniforms(GL2ES2 gl) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");        
        activeUniformLocationMap.clear();
        for(Iterator<GLUniformData> iter = managedUniforms.iterator(); iter.hasNext(); ) {
            iter.next().setLocation(-1);
        }        
        for(Iterator<GLUniformData> iter = activeUniformDataMap.values().iterator(); iter.hasNext(); ) {
            final GLUniformData uniform = iter.next();
            uniform.setLocation(-1);
            uniform(gl, uniform);
        }
    }

    public StringBuilder toString(StringBuilder sb) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        
        sb.append("ShaderState[ ");
        
        sb.append(Platform.getNewline()).append(" ");
        if(null != shaderProgram) {
            shaderProgram.toString(sb);
        } else {
            sb.append("ShaderProgram: null");
        }
        sb.append(Platform.getNewline()).append(" enabledAttributes [");
        for(Iterator<String> iter = enabledAttributes.iterator(); iter.hasNext(); ) {
            sb.append(Platform.getNewline()).append("  ").append(iter.next());
        }
        sb.append(Platform.getNewline()).append(" ],").append(" activeAttributes [");
        for(Iterator<GLArrayData> iter = activeAttribDataMap.values().iterator(); iter.hasNext(); ) {
            sb.append(Platform.getNewline()).append("  ").append(iter.next());
        }
        sb.append(Platform.getNewline()).append(" ],").append(" managedAttributes [");
        for(Iterator<GLArrayData> iter = managedAttributes.iterator(); iter.hasNext(); ) {
            sb.append(Platform.getNewline()).append("  ").append(iter.next());
        }
        sb.append(Platform.getNewline()).append(" ],").append(" activeUniforms [");
        for(Iterator<GLUniformData> iter=activeUniformDataMap.values().iterator(); iter.hasNext(); ) {
            sb.append(Platform.getNewline()).append("  ").append(iter.next());
        }
        sb.append(Platform.getNewline()).append(" ],").append(" managedUniforms [");
        for(Iterator<GLUniformData> iter = managedUniforms.iterator(); iter.hasNext(); ) {
            sb.append(Platform.getNewline()).append("  ").append(iter.next());
        }
        sb.append(Platform.getNewline()).append(" ]").append(Platform.getNewline()).append("]");
        return sb;
    }
    
    @Override
    public String toString() {
        return toString(null).toString();
    }
    
    private boolean verbose = false;
    private ShaderProgram shaderProgram=null;
    
    private HashSet<String> enabledAttributes = new HashSet<String>();
    private HashMap<String, Integer> activeAttribLocationMap = new HashMap<String, Integer>();
    private HashMap<String, GLArrayData> activeAttribDataMap = new HashMap<String, GLArrayData>();
    private ArrayList<GLArrayData> managedAttributes = new ArrayList<GLArrayData>();
    
    private HashMap<String, Integer> activeUniformLocationMap = new HashMap<String, Integer>();
    private HashMap<String, GLUniformData> activeUniformDataMap = new HashMap<String, GLUniformData>();
    private ArrayList<GLUniformData> managedUniforms = new ArrayList<GLUniformData>();
    
    private HashMap<String, Object> attachedObjectsByString = new HashMap<String, Object>();    
    private IntObjectHashMap attachedObjectsByInt = new IntObjectHashMap();   
    private boolean resetAllShaderData = false;
}

