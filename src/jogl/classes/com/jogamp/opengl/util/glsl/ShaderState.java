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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLArrayData;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLUniformData;

import jogamp.opengl.Debug;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.opengl.util.GLArrayDataEditable;

/**
 * ShaderState allows to sharing data between shader programs,
 * while updating the attribute and uniform locations when switching.
 * <p>
 * This allows seamless switching of programs using <i>almost</i> same data
 * but performing different artifacts.
 * </p>
 * <p>
 * A {@link #useProgram(GL2ES2, boolean) used} ShaderState is attached to the current GL context
 * and can be retrieved via {@link #getShaderState(GL)}.
 * </p>
 */
public final class ShaderState {
    public static final boolean DEBUG;

    static {
        Debug.initSingleton();
        DEBUG = PropertyAccess.isPropertyDefined("jogl.debug.GLSLState", true);
    }

    public ShaderState() {
    }

    public boolean verbose() { return verbose; }

    public void setVerbose(final boolean v) { verbose = DEBUG || v; }

    /**
     * Returns the attached user object for the given name to this ShaderState.
     */
    public final Object getAttachedObject(final String name) {
      return attachedObjectsByString.get(name);
    }

    /**
     * Attach user object for the given name to this ShaderState.
     * Returns the previously set object or null.
     *
     * @return the previous mapped object or null if none
     */
    public final Object attachObject(final String name, final Object obj) {
      return attachedObjectsByString.put(name, obj);
    }

    /**
     * @param name name of the mapped object to detach
     *
     * @return the previous mapped object or null if none
     */
    public final Object detachObject(final String name) {
        return attachedObjectsByString.remove(name);
    }

    /**
     * Turns the shader program on or off.<br>
     *
     * @throws GLException if no program is attached
     *
     * @see com.jogamp.opengl.util.glsl.ShaderState#useProgram(GL2ES2, boolean)
     */
    public synchronized void useProgram(final GL2ES2 gl, final boolean on) throws GLException {
        if(null==shaderProgram) { throw new GLException("No program is attached"); }
        if(on) {
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
     * <p>[Re]sets all data and use program in case of a program switch.</p>
     *
     * <p>Use program, {@link #useProgram(GL2ES2, boolean)},
     * if <code>enable</code> is <code>true</code>.</p>
     *
     * @return true if shader program was attached, otherwise false (already attached)
     *
     * @throws GLException if program was not linked and linking fails
     */
    public synchronized boolean attachShaderProgram(final GL2ES2 gl, final ShaderProgram prog, final boolean enable) throws GLException {
        if(verbose) {
            final int curId = (null!=shaderProgram)?shaderProgram.id():-1;
            final int newId = (null!=prog)?prog.id():-1;
            System.err.println("ShaderState: attachShaderProgram: "+curId+" -> "+newId+" (enable: "+enable+")\n\t"+shaderProgram+"\n\t"+prog);
            // System.err.println(toString());
            /*
            if(DEBUG) {
                ExceptionUtils.dumpStack(System.err);
            } */
        }
        if(null!=shaderProgram) {
            if(shaderProgram.equals(prog)) {
                if(enable) {
                    useProgram(gl, true);
                }
                // nothing else to do ..
                if(verbose) {
                    System.err.println("ShaderState: attachShaderProgram: No switch, equal id: "+shaderProgram.id()+", enabling "+enable);
                }
                return false;
            }
            if(shaderProgram.inUse()) {
                if(null != prog && enable) {
                    shaderProgram.notifyNotInUse();
                } else {
                    // no new 'enabled' program - disable
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
            if(resetAllShaderData || enable) {
                useProgram(gl, true); // may reset all data
                if(!enable) {
                    useProgram(gl, false);
                }
            }
        }
        if(DEBUG) {
            System.err.println("Info: attachShaderProgram: END");
            // System.err.println(toString());
        }
        return true;
    }

    public ShaderProgram shaderProgram() { return shaderProgram; }

    /**
     * Calls {@link #release(GL2ES2, boolean, boolean, boolean) release(gl, true, true, true)}
     *
     * @see #releaseAllAttributes
     * @see #releaseAllUniforms
     * @see #release(GL2ES2, boolean, boolean, boolean)
     */
    public synchronized void destroy(final GL2ES2 gl) {
        release(gl, true, true, true);
        attachedObjectsByString.clear();
    }

    /**
     * Calls {@link #release(GL2ES2, boolean, boolean, boolean) release(gl, false, false, false)}
     *
     * @see #releaseAllAttributes
     * @see #releaseAllUniforms
     * @see #release(GL2ES2, boolean, boolean, boolean)
     */
    public synchronized void releaseAllData(final GL2ES2 gl) {
        release(gl, false, false, false);
    }

    /**
     * @see #releaseAllAttributes
     * @see #releaseAllUniforms
     * @see ShaderProgram#release(GL2ES2, boolean)
     */
    public synchronized void release(final GL2ES2 gl, final boolean destroyBoundAttributes, final boolean destroyShaderProgram, final boolean destroyShaderCode) {
        if(null!=shaderProgram && shaderProgram.linked() ) {
            shaderProgram.useProgram(gl, false);
        }
        if(destroyBoundAttributes) {
            for(final Iterator<GLArrayData> iter = managedAttributes.iterator(); iter.hasNext(); ) {
                iter.next().destroy(gl);
            }
        }
        releaseAllAttributes(gl);
        releaseAllUniforms(gl);
        if(null!=shaderProgram && destroyShaderProgram) {
            shaderProgram.release(gl, destroyShaderCode);
        }
    }

    //
    // Shader attribute handling
    //

    /**
     * Get the previous cached vertex attribute data.
     *
     * @return the GLArrayData object, null if not previously set.
     *
     * @see #manage(GLArrayData, boolean)
     * @see #enableVertexAttribArray
     * @see #disableVertexAttribArray
     * @see #vertexAttribPointer
     * @see #releaseAllVertexAttributes
     * @see #resetAllAttributes
     * @see ShaderProgram#replaceShader(GL2ES2, ShaderCode, ShaderCode, java.io.PrintStream)
     */
    public GLArrayData getAttribute(final String name) {
        final DataLoc dl = activeAttribDataMap.get(name);
        return null != dl ? dl.data : null;
    }
    /**
     * Get the previous cached vertex attribute location.
     *
     * @return the location or -1 if not previously set.
     *
     * @see #manage(GLArrayData, boolean)
     * @see #enableVertexAttribArray
     * @see #disableVertexAttribArray
     * @see #vertexAttribPointer
     * @see #releaseAllVertexAttributes
     * @see #resetAllAttributes
     * @see ShaderProgram#replaceShader(GL2ES2, ShaderCode, ShaderCode, java.io.PrintStream)
     */
    public int getAttributeLocation(final String name) {
        final DataLoc dl = activeAttribDataMap.get(name);
        return null != dl ? dl.location : -1;
    }

    private void updateAttributeCache(final GLArrayData data) {
        updateAttributeCache(data, -1);
    }
    private DataLoc updateAttributeCache(final GLArrayData data, final int enabledVal) {
        return updateDataLoc(activeAttribDataMap.get(data.getName()), data, enabledVal, true);
    }
    private DataLoc updateDataLoc(DataLoc dl, final GLArrayData data, final int enabledVal, final boolean mapNewInstance) {
        if( null != dl ) {
            if( null != dl.data && dl.data == data ) {
                dl.location = data.getLocation();
            } else {
                // no or different previous data object
                dl.data = data;
                dl.location = data.getLocation();
            }
        } else {
            // new instance
            dl = new DataLoc(data);
            if( mapNewInstance) {
                activeAttribDataMap.put(data.getName(), dl);
            }
        }
        dl.setEnabled(enabledVal);
        return dl;
    }
    private DataLoc updateDataLoc(DataLoc dl, final String name, final int location, final int enabledVal, final boolean mapNewInstance) {
        if( null != dl ) {
            if( null != dl.data ) {
                dl.data.setLocation(location);
            }
            dl.location = location;
        } else {
            dl = new DataLoc(name, location);
            if( mapNewInstance) {
                activeAttribDataMap.put(name, dl);
            }
        }
        dl.setEnabled(enabledVal);
        return dl;
    }

    public boolean isActive(final GLArrayData data) {
        final DataLoc dl = activeAttribDataMap.get(data.getName());
        return null != dl && dl.data == data;
    }
    public boolean isActive(final String name) {
        return null != activeAttribDataMap.get(name);
    }

    /**
     * Binds or unbinds the {@link GLArrayData} lifecycle to this ShaderState.
     *
     * <p>The attribute will be destroyed with {@link #destroy(GL2ES2)}
     * and it's location will be reset when switching shader with {@link #attachShaderProgram(GL2ES2, ShaderProgram)}.</p>
     *
     * <p>The data will not be transfered to the GPU, use {@link #vertexAttribPointer(GL2ES2, GLArrayData)} additionally.</p>
     *
     * <p>The data will also be {@link GLArrayData#associate(Object, boolean) associated} with this ShaderState.</p>
     *
     * @param attribute the {@link GLArrayData} which lifecycle shall be managed
     * @param enable true if <i>owning</i> shall be performs, false if <i>disowning</i>.
     *
     * @see #bindAttribLocation(GL2ES2, int, String)
     * @see #getAttribute(String)
     * @see GLArrayData#associate(Object, boolean)
     */
    public void manage(final GLArrayData attribute, final boolean enable) {
        if(enable) {
            managedAttributes.add(managedAttributes.size(), attribute);
        } else {
            managedAttributes.remove(attribute);
        }
        attribute.associate(this, enable);
    }

    public boolean isOwned(final GLArrayData attribute) {
        return managedAttributes.contains(attribute);
    }

    /**
     * Binds a shader {@link GLArrayData} attribute to a location.
     * Multiple names can be bound to one location.
     * The value will be cached and can be retrieved via {@link #getCachedAttribLocation(String)}
     * and {@link #getAttribute(String)}before or after linking.
     * The {@link GLArrayData}'s location will be set as well.
     *
     * @throws GLException if no program is attached
     * @throws GLException if the program is already linked
     *
     * @see com.jogamp.opengl.GL2ES2#glBindAttribLocation(int, int, String)
     * @see #resolveLocation(GL2ES2, String)
     * @see #getCachedAttribLocation(String)
     * @see #getAttribute(String)
     */
    public void bindAttribLocation(final GL2ES2 gl, final int location, final GLArrayData data) {
        if(null==shaderProgram) throw new GLException("No program is attached");
        if(shaderProgram.linked()) throw new GLException("Program is already linked");
        final String name = data.getName();
        data.setLocation(gl, shaderProgram.program(), location);
        updateAttributeCache(data);
        gl.glBindAttribLocation(shaderProgram.program(), location, name);
    }

    /**
     * Validates the location of a shader attribute.
     *
     * If the cashed {@link #getAttributeLocation(String)} is invalid,
     * it is queried via {@link GL2ES2#glGetAttribLocation(int, String)} (GLSL).
     *
     * The location will be cached.
     *
     * @return -1 if there is no such attribute available,
     *         otherwise >= 0
     * @throws GLException if no program is attached
     * @throws GLException if the program is not linked and no location was cached.
     *
     * @see #getCachedAttribLocation(String)
     * @see #bindAttribLocation(GL2ES2, int, GLArrayData)
     * @see #bindAttribLocation(GL2ES2, int, String)
     * @see GL2ES2#glGetAttribLocation(int, String)
     */
    public int resolveLocation(final GL2ES2 gl, final String name) {
        final DataLoc dl = resolveLocation2(gl, name, false);
        return dl.location;
    }
    private DataLoc resolveLocation2(final GL2ES2 gl, final String name, final boolean forceMap) {
        DataLoc dl = activeAttribDataMap.get(name);
        int location = null != dl ? dl.location : -1;
        if( 0 > location ) {
            if(null==shaderProgram) throw new GLException("No program is attached");
            if(!shaderProgram.linked()) throw new GLException("Program is not linked");
            location = gl.glGetAttribLocation(shaderProgram.program(), name);
            dl = updateDataLoc(dl, name, location, -1, false);
            if( 0 <= location ) {
                activeAttribDataMap.put(name, dl);
                if(DEBUG) {
                    System.err.println("ShaderState: resolveAttributeLocation(1): "+name+", loc: "+location);
                }
            } else {
                if( forceMap ) {
                    activeAttribDataMap.put(name, dl);
                }
                if( verbose ) {
                    System.err.println("ShaderState: resolveAttributeLocation(1) failed, no location for: "+name+", loc: "+location);
                    if(DEBUG) {
                        ExceptionUtils.dumpStack(System.err);
                    }
                }
            }
        }
        return dl;
    }

    /**
     * Validates the location of a shader attribute.
     *
     * If {@link GLArrayData#getLocation()} or the cashed {@link #getAttributeLocation(String)} is invalid,
     * it is queried via {@link GLArrayData#resolveLocation(GL2ES2, int)} (GLSL).
     *
     * The location will be cached.
     *
     * @return true if successful, otherwise false
     *
     * @throws GLException if no program is attached
     * @throws GLException if the program is not linked and no location was cached.
     *
     * @see #getCachedAttribLocation(String)
     * @see #bindAttribLocation(GL2ES2, int, GLArrayData)
     * @see #bindAttribLocation(GL2ES2, int, String)
     * @see GL2ES2#glGetAttribLocation(int, String)
     * @see #getAttribute(String)
     */
    public boolean resolveLocation(final GL2ES2 gl, final GLArrayData data) {
        resolveLocation2(gl, data, false);
        return data.hasLocation();
    }
    private DataLoc resolveLocation2(final GL2ES2 gl, final GLArrayData data, final boolean forceMap) {
        if ( data.hasLocation() ) {
            return updateAttributeCache(data, -1);
        }
        DataLoc dl = activeAttribDataMap.get(data.getName());
        final int location = null != dl ? dl.location : -1;
        if( 0 <= location ) {
            data.setLocation(location);
        } else {
            if (null==shaderProgram) throw new GLException("No program is attached");
            if (!shaderProgram.linked()) throw new GLException("Program is not linked");
            if ( data.resolveLocation(gl, shaderProgram.program()) ) {
                dl = updateDataLoc(dl, data, -1, true);
                if(DEBUG) {
                    System.err.println("ShaderState: resolveAttributeLocation(2): "+data.getName()+", loc: "+data.getLocation());
                }
            } else {
                dl = updateDataLoc(dl, data, -1, forceMap);
                if (verbose) {
                    System.err.println("ShaderState: resolveAttributeLocation(2) failed, no location for: " + data.getName());
                    if (DEBUG) {
                        ExceptionUtils.dumpStack(System.err);
                    }
                }
            }
        }
        return dl;
    }

    //
    // Enabled Vertex Arrays and its data
    //

    /**
     * @return true if the named attribute is enable
     */
    public final boolean isVertexAttribArrayEnabled(final String name) {
        final DataLoc dl = activeAttribDataMap.get(name);
        return null != dl ? dl.enabled : false;
    }

    /**
     * @return true if the {@link GLArrayData} attribute is enable
     */
    public final boolean isVertexAttribArrayEnabled(final GLArrayData data) {
        return isVertexAttribArrayEnabled(data.getName());
    }

    /**
     * Enables a vertex attribute array.
     *
     * This method retrieves the the location via {@link #resolveLocation(GL2ES2, GLArrayData)}
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
    public boolean enableVertexAttribArray(final GL2ES2 gl, final String name) {
        final DataLoc dl = resolveLocation2(gl, name, true);
        dl.setEnabled(1);
        final int location = dl.location;
        if( 0 > location ) {
            if(verbose) {
                System.err.println("ShaderState: enableVertexAttribArray(1) failed, no index for: "+name);
                if(DEBUG) {
                    ExceptionUtils.dumpStack(System.err);
                }
            }
            return false;
        }
        if(DEBUG) {
            System.err.println("ShaderState: enableVertexAttribArray(1): "+name+", loc: "+location);
        }
        gl.glEnableVertexAttribArray(location);
        return true;
    }


    /**
     * Enables a vertex attribute array, usually invoked by {@link GLArrayDataEditable#enableBuffer(GL, boolean)}.
     *
     * This method uses the {@link GLArrayData}'s location if set
     * and is the preferred alternative to {@link #enableVertexAttribArray(GL2ES2, String)}.
     * If data location is unset it will be retrieved via {@link #resolveLocation(GL2ES2, GLArrayData)} set
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
    public boolean enableVertexAttribArray(final GL2ES2 gl, final GLArrayData data) {
        final DataLoc dl = resolveLocation2(gl, data, true);
        dl.setEnabled(1);
        final int location = dl.location;
        if( 0 > location ) {
            if(verbose) {
                System.err.println("ShaderState: enableVertexAttribArray(2) failed, no index for: "+data.getName());
                if(DEBUG) {
                    ExceptionUtils.dumpStack(System.err);
                }
            }
            return false;
        }
        if(DEBUG) {
            System.err.println("ShaderState: enableVertexAttribArray(2): "+data.getName()+", loc: "+data.getLocation());
        }
        gl.glEnableVertexAttribArray(data.getLocation());
        return true;
    }

    /**
     * Disables a vertex attribute array
     *
     * This method retrieves the the location via {@link #resolveLocation(GL2ES2, GLArrayData)}
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
    public boolean disableVertexAttribArray(final GL2ES2 gl, final String name) {
        final DataLoc dl = resolveLocation2(gl, name, false);
        dl.setEnabled(0);
        final int location = dl.location;
        if( 0 > location ) {
            if(verbose) {
                System.err.println("ShaderState: disableVertexAttribArray(1) failed, no index for: "+name);
                if(DEBUG) {
                    ExceptionUtils.dumpStack(System.err);
                }
            }
            return false;
        }
        if(DEBUG) {
            System.err.println("ShaderState: disableVertexAttribArray(1): "+name+", loc: "+location);
        }
        gl.glDisableVertexAttribArray(location);
        return true;
    }

    /**
     * Disables a vertex attribute array
     *
     * This method uses the {@link GLArrayData}'s location if set
     * and is the preferred alternative to {@link #disableVertexAttribArray(GL2ES2, String)}.
     * If data location is unset it will be retrieved via {@link #resolveLocation(GL2ES2, GLArrayData)} set
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
    public boolean disableVertexAttribArray(final GL2ES2 gl, final GLArrayData data) {
        final DataLoc dl = resolveLocation2(gl, data, false);
        dl.setEnabled(0);
        final int location = dl.location;
        if( 0 > location ) {
            if(verbose) {
                System.err.println("ShaderState: disableVertexAttribArray(2) failed, no index for: "+data.getName());
                if(DEBUG) {
                    ExceptionUtils.dumpStack(System.err);
                }
            }
            return false;
        }
        if(DEBUG) {
            System.err.println("ShaderState: disableVertexAttribArray(2): "+data.getName()+", loc: "+data.getLocation());
        }
        gl.glDisableVertexAttribArray(data.getLocation());
        return true;
    }

    /**
     * Set the {@link GLArrayData} vertex attribute data, if it's location is valid, i.e. &ge; 0.
     * <p>
     * This method uses the {@link GLArrayData}'s location if valid, i.e. &ge; 0.<br/>
     * If data's location is invalid, it will be retrieved via {@link #resolveLocation(GL2ES2, GLArrayData)},
     * set and cached in this state.
     * </p>
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
    public boolean vertexAttribPointer(final GL2ES2 gl, final GLArrayData data) {
        resolveLocation2(gl, data, true); // always map
        if(!data.hasLocation()) {
            return false;
        }
        gl.glVertexAttribPointer(data);
        return true;
    }

    /**
     * Releases all mapped vertex attribute data,
     * disables all enabled attributes and loses all locations
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
    public void releaseAllAttributes(final GL2ES2 gl) {
        for(final Iterator<DataLoc> iter = activeAttribDataMap.values().iterator(); iter.hasNext(); ) {
            final DataLoc dl = iter.next();
            if( null != dl.data ) {
                final GLArrayData d = dl.data;
                if ( resolveLocation(gl, d) ) {
                    gl.glDisableVertexAttribArray(d.getLocation());
                    d.setLocation(-1);
                }
            } else if( dl.location >= 0 ) {
                gl.glDisableVertexAttribArray(dl.location);
            }
        }
        activeAttribDataMap.clear();
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
    public void disableAllVertexAttributeArrays(final GL2ES2 gl, final boolean removeFromState) {
        for(final Iterator<DataLoc> iter = activeAttribDataMap.values().iterator(); iter.hasNext(); ) {
            final DataLoc dl = iter.next();
            if( null != dl.data ) {
                final GLArrayData d = dl.data;
                if ( resolveLocation(gl, d) ) {
                    gl.glDisableVertexAttribArray(d.getLocation());
                }
            } else if( dl.location >= 0 ) {
                gl.glDisableVertexAttribArray(dl.location);
            }
            if( removeFromState ) {
                dl.setEnabled(0);
            }
        }
    }

    private final boolean relocateAttribute(final GL2ES2 gl, final DataLoc dl) {
        final GLArrayData data = dl.data;
        final String name = data.getName();
        if( data.resolveLocation(gl, shaderProgram.program()) ) {
            if(DEBUG) {
                System.err.println("ShaderState: relocateAttribute: "+name+", loc: "+data.getLocation());
            }
            if(dl.enabled) {
                // enable attrib, VBO and pass location/data
                gl.glEnableVertexAttribArray(data.getLocation());
            }

            if( data.isVBO() ) {
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, data.getVBOName());
                gl.glVertexAttribPointer(data);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            } else {
                gl.glVertexAttribPointer(data);
            }
            return true;
        }
        return false;
    }

    /**
     * Reset all previously enabled mapped vertex attribute data.
     *
     * <p>
     * Attribute data is bound to the GL state, i.e. VBO data itself will not be updated.
     * </p>
     *
     * <p>
     * Attribute location and it's data assignment is bound to the program,
     * hence both are updated.
     * </p>
     *
     * <p>
     * Note: Such update could only be prevented,
     * if tracking am attribute/program dirty flag.
     * </p>
     *
     * @throws GLException is the program is not linked
     *
     * @see #attachShaderProgram(GL2ES2, ShaderProgram)
     */
    private final void resetAllAttributes(final GL2ES2 gl) {
        if(!shaderProgram.linked()) throw new GLException("Program is not linked");

        for(int i=0; i<managedAttributes.size(); i++) {
            managedAttributes.get(i).setLocation(-1);
        }
        for(final Iterator<DataLoc> iter = activeAttribDataMap.values().iterator(); iter.hasNext(); ) {
            final DataLoc dl = iter.next();
            if( null != dl.data ) {
                if( relocateAttribute(gl, dl) ) {
                    dl.location = dl.data.getLocation();
                } else {
                    dl.location = -1;
                }
            } else {
                dl.location = -1;
            }
        }
    }

    private final void setAttribute(final GL2ES2 gl, final DataLoc dl) {
        // get new location ..
        final GLArrayData data = dl.data;
        final String name = data.getName();
        final int loc = data.getLocation();

        if(0<=loc) {
            gl.glBindAttribLocation(shaderProgram.program(), loc, name);

            if(dl.enabled) {
                // enable attrib, VBO and pass location/data
                gl.glEnableVertexAttribArray(loc);
            }

            if( data.isVBO() ) {
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, data.getVBOName());
                gl.glVertexAttribPointer(data);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            } else {
                gl.glVertexAttribPointer(data);
            }
        }
    }

    /**
     * preserves the attribute location .. (program not linked)
     */
    private final void setAllAttributes(final GL2ES2 gl) {
        for(final Iterator<DataLoc> iter = activeAttribDataMap.values().iterator(); iter.hasNext(); ) {
            final DataLoc dl = iter.next();
            if( null != dl.data ) {
                setAttribute(gl, dl);
            }
        }
    }

    //
    // Shader Uniform handling
    //

    /**
     * Bind the {@link GLUniform} lifecycle to this ShaderState.
     *
     * <p>The attribute will be destroyed with {@link #destroy(GL2ES2)}
     * and it's location will be reset when switching shader with {@link #attachShaderProgram(GL2ES2, ShaderProgram)}.</p>
     *
     * <p>The data will not be transfered to the GPU, use {@link #send(GL2ES2, GLUniformData)} additionally.</p>
     *
     * @param uniform the {@link GLUniformData} which lifecycle shall be managed
     * @param enable TODO
     *
     * @see #getActiveUniform(String)
     */
    public void manage(final GLUniformData uniform, final boolean enable) {
        if(enable) {
            activeUniformDataMap.put(uniform.getName(), uniform);
            managedUniforms.add(uniform);
        } else {
            activeUniformDataMap.remove(uniform.getName());
            managedUniforms.remove(uniform);
        }
    }

    public boolean isManaged(final GLUniformData uniform) {
        return managedUniforms.contains(uniform);
    }

    /**
     * Validates the location of a shader uniform.<br>
     * Uses either the internal value {@link GLUniformData#getLocation()} if valid,
     * or the GLSL queried via {@link GL2ES2#glGetUniformLocation(int, String)}.<br>
     * The location will be cached and set in the
     * {@link GLUniformData} object.
     * <p>
     * The current shader program ({@link #attachShaderProgram(GL2ES2, ShaderProgram)})
     * must be in use ({@link #useProgram(GL2ES2, boolean) }) !</p>
     *
     * @return true if successful, otherwise false

     * @throws GLException is the program is not in use
     *
     * @see #glGetUniformLocation
     * @see com.jogamp.opengl.GL2ES2#glGetUniformLocation
     * @see ShaderProgram#glReplaceShader
     */
    public boolean resolveLocation(final GL2ES2 gl, final GLUniformData data) {
        if (!data.hasLocation()) {
            if (null==shaderProgram) throw new GLException("No program is attached");
            if (!shaderProgram.inUse()) throw new GLException("Program is not in use");
            if (0 > data.setLocation(gl, shaderProgram.program()) ) {
                if (verbose()) {
                    System.err.println("ShaderState: glUniform failed, no location for: "+data.getName());
                }
            }
        }
        activeUniformDataMap.put(data.getName(), data);
        return data.hasLocation();
    }

    /**
     * Sends the uniform data to the GPU if it's location is valid, i.e. &ge; 0.
     * <p>
     * This method uses the {@link GLUniformData}'s location if valid, i.e. &ge; 0.<br/>
     * If data's location is invalid, it will be retrieved via {@link #resolveLocation(GL2ES2, GLUniformData)},
     * set and cached in this state.
     * </p>
     *
     * @return false, if the location could not be determined, otherwise true
     *
     * @see #glGetUniformLocation
     * @see com.jogamp.opengl.GL2ES2#glGetUniformLocation
     * @see com.jogamp.opengl.GL2ES2#glUniform
     * @see #getUniformLocation
     * @see ShaderProgram#glReplaceShader
     */
    public boolean send(final GL2ES2 gl, final GLUniformData data) {
        if (!data.hasLocation() && !resolveLocation(gl, data)) {
            return false;
        }
        // only pass the data, if the uniform exists in the current shader
        gl.glUniform(data);
        return true;
    }

    /// Returns true if given uniform data is active, i.e. previously resolved/send and used in current program.
    public boolean isActive(final GLUniformData uniform) {
        return uniform == activeUniformDataMap.get(uniform.getName());
    }

    /**
     * Get the uniform data, previously set.
     *
     * @return the GLUniformData object, null if not previously set.
     */
    public GLUniformData getActiveUniform(final String name) {
        return activeUniformDataMap.get(name);
    }

    /**
     * Releases all mapped uniform data
     * and loses all indices
     */
    public void releaseAllUniforms(final GL2ES2 gl) {
        activeUniformDataMap.clear();
        managedUniforms.clear();
    }

    /**
     * Reset all previously mapped uniform data
     * <p>
     * Uniform data and location is bound to the program,
     * hence both are updated.
     * </p>
     * <p>
     * Note: Such update could only be prevented,
     * if tracking a uniform/program dirty flag.
     * </p>
     *
     * @throws GLException is the program is not in use
     *
     * @see #attachShaderProgram(GL2ES2, ShaderProgram)
     */
    private final void resetAllUniforms(final GL2ES2 gl) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        for(final Iterator<GLUniformData> iter = managedUniforms.iterator(); iter.hasNext(); ) {
            iter.next().setLocation(-1);
        }
        for(final Iterator<GLUniformData> iter = activeUniformDataMap.values().iterator(); iter.hasNext(); ) {
            final GLUniformData data = iter.next();
            final int loc = data.setLocation(gl, shaderProgram.program());
            if( 0 <= loc ) {
                // only pass the data, if the uniform exists in the current shader
                if(DEBUG) {
                    System.err.println("ShaderState: resetAllUniforms: "+data);
                }
                gl.glUniform(data);
            }
        }
    }

    public StringBuilder toString(StringBuilder sb, final boolean alsoUnlocated) {
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
        sb.append(Platform.getNewline()).append(" ],").append(" activeAttributes [");
        {
            final Set<Map.Entry<String,DataLoc>> entries = activeAttribDataMap.entrySet();
            for(final Map.Entry<String,DataLoc> e : entries) {
                final DataLoc dl = e.getValue();
                sb.append(Platform.getNewline()).append("  ").append(e.getKey())
                  .append(": enabled ").append(dl.enabled).append(", ");
                if( null != dl.data ) {
                    sb.append(dl.data);
                } else {
                    sb.append("location ").append(dl.location);
                }
            }
        }
        sb.append(Platform.getNewline()).append(" ],").append(" managedAttributes [");
        for(final Iterator<GLArrayData> iter = managedAttributes.iterator(); iter.hasNext(); ) {
            final GLArrayData ad = iter.next();
            sb.append(Platform.getNewline()).append("  ").append(ad);
        }
        sb.append(Platform.getNewline()).append(" ],").append(" activeUniforms [");
        for(final Iterator<GLUniformData> iter=activeUniformDataMap.values().iterator(); iter.hasNext(); ) {
            final GLUniformData ud = iter.next();
            if( alsoUnlocated || 0 <= ud.getLocation() ) {
                sb.append(Platform.getNewline()).append("  ").append(ud);
            }
        }
        sb.append(Platform.getNewline()).append(" ],").append(" managedUniforms [");
        for(final Iterator<GLUniformData> iter = managedUniforms.iterator(); iter.hasNext(); ) {
            final GLUniformData ud = iter.next();
            sb.append(Platform.getNewline()).append("  ").append(ud);
        }
        sb.append(Platform.getNewline()).append(" ]").append(Platform.getNewline()).append("]");
        return sb;
    }

    @Override
    public String toString() {
        return toString(null, DEBUG).toString();
    }

    private boolean verbose = DEBUG;
    private ShaderProgram shaderProgram=null;

    private static class DataLoc {
        String name;
        GLArrayData data;
        int location;
        boolean enabled;
        DataLoc(final String name_, final int l) {
            name = name_;
            data = null;
            location = l;
            enabled = false;
            if( DEBUG ) {
                System.err.println("DataLoc.ctor0: "+toString());
            }
        }
        DataLoc(final GLArrayData d) {
            name = d.getName();
            data = d;
            location = d.getLocation();
            enabled = false;
            if( DEBUG ) {
                System.err.println("DataLoc.ctor1: "+toString());
            }
        }
        void setEnabled(final int enabledVal) {
            if( 0 <= enabledVal ) {
                enabled = enabledVal > 0;
            }
        }
        @Override
        public String toString() {
            return name+", loc "+location+", enabled "+enabled;
        }
    }
    private final HashMap<String, DataLoc> activeAttribDataMap = new HashMap<String, DataLoc>();
    private final ArrayList<GLArrayData> managedAttributes = new ArrayList<GLArrayData>();

    private final HashMap<String, GLUniformData> activeUniformDataMap = new HashMap<String, GLUniformData>();
    private final ArrayList<GLUniformData> managedUniforms = new ArrayList<GLUniformData>();

    private final HashMap<String, Object> attachedObjectsByString = new HashMap<String, Object>();
    private boolean resetAllShaderData = false;
}

