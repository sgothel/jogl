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

import javax.media.opengl.*;

import java.util.HashMap;
import java.util.Iterator;
import java.io.PrintStream;

public class ShaderProgram {
    public ShaderProgram() {
        id = getNextID();
    }

    public boolean linked() {
        return programLinked;
    }

    public boolean inUse() {
        return programInUse;
    }

    public int program() { return shaderProgram; }

    /**
     * returns the uniq shader id as an integer
     * @see #key()
     */
    public int        id() { return id.intValue(); }

    /**
     * returns the uniq shader id as an Integer
     *
     * @see #id()
     */
    public Integer    key() { return id; }

    /**
     * Detaches all shader codes and deletes the program.
     * Destroys the shader codes as well.
     * Calls release(gl, true)
     *
     * @see #release(GL2ES2, boolean)
     */
    public synchronized void destroy(GL2ES2 gl) {
        release(gl, true);
    }

    /**
     * Detaches all shader codes and deletes the program.
     * Calls release(gl, false)
     *
     * @see #release(GL2ES2, boolean)
     */
    public synchronized void release(GL2ES2 gl) {
        release(gl, false);
    }

    /**
     * Detaches all shader codes and deletes the program.
     * If releaseShaderToo is true, destroys the shader codes as well.
     */
    public synchronized void release(GL2ES2 gl, boolean releaseShaderToo) {
        glUseProgram(gl, false);
        for(Iterator iter=shaderMap.values().iterator(); iter.hasNext(); ) {
            ShaderCode shaderCode = (ShaderCode) iter.next();
            ShaderUtil.detachShader(gl, shaderProgram, shaderCode.shader());
            if(releaseShaderToo) {
                shaderCode.destroy(gl);
            }
        }
        shaderMap.clear();
        gl.glDeleteProgram(shaderProgram);
        shaderProgram=-1;
    }

    //
    // ShaderCode handling
    //

    /**
     * Adds a new shader to a this non running program.
     *
     * @return false if the program is in use, or the shader already exist,
     *         otherwise true.
     */
    public synchronized boolean add(ShaderCode shaderCode) {
        if(shaderMap.containsKey(shaderCode.key())) return false;
        shaderMap.put(shaderCode.key(), shaderCode);
        return true;
    }

    public synchronized ShaderCode getShader(int id) {
        return (ShaderCode) shaderMap.get(new Integer(id));
    }

    //
    // Program handling
    //

    /**
     * Replace a shader in a 'running' program.
     * Refetches all previously bin/get attribute names
     * and resets all attribute data as well
     *
     * @param gl 
     * @param oldShaderID the to be replace Shader
     * @param newShader   the new ShaderCode
     * @param verboseOut  the optional verbose outputstream
     * @throws GLException is the program is not linked
     *
     * @see ShaderState#glEnableVertexAttribArray
     * @see ShaderState#glDisableVertexAttribArray
     * @see ShaderState#glVertexAttribPointer
     * @see ShaderState#getVertexAttribPointer
     * @see ShaderState#glReleaseAllVertexAttributes
     * @see ShaderState#glResetAllVertexAttributes
     * @see ShaderState#glResetAllVertexAttributes
     * @see ShaderState#glResetAllVertexAttributes
     */
    public synchronized boolean glReplaceShader(GL2ES2 gl, int oldShaderID, ShaderCode newShader, PrintStream verboseOut) {
        if(!programLinked) throw new GLException("Program is not linked");
        boolean shaderWasInUse = programInUse;
        glUseProgram(gl, false);
        if(!newShader.compile(gl, verboseOut)) {
            return false;
        } 
        if(oldShaderID>=0) {
            ShaderCode oldShader = (ShaderCode) shaderMap.remove(new Integer(oldShaderID));
            if(null!=oldShader) {
                ShaderUtil.detachShader(gl, shaderProgram, oldShader.shader());
            }
        }
        add(newShader);

        ShaderUtil.attachShader(gl, shaderProgram, newShader.shader());
        gl.glLinkProgram(shaderProgram);
        if ( ! ShaderUtil.isProgramValid(gl, shaderProgram, System.err) )  {
            return false;
        }

        if(shaderWasInUse) {
            glUseProgram(gl, true);
        }
        return true;
    }

    /**
     * Creates the empty GL program object using {@link GL2ES2#glCreateProgram()}
     *  
     * @param gl
     */
    public final void init(GL2ES2 gl) {
        if(0>shaderProgram) {
            shaderProgram = gl.glCreateProgram();
        }
    }
    
    /**
     * Compiles and links the shader code to the program.
     * Within this process, all GL resources (shader and program objects) are created if necessary.
     *  
     * @param gl
     * @param verboseOut
     * @return
     * 
     * @see #init(GL2ES2)
     */
    public synchronized boolean link(GL2ES2 gl, PrintStream verboseOut) {
        if(programLinked) throw new GLException("Program is already linked");

        if(0>shaderProgram) {
            shaderProgram = gl.glCreateProgram();
        }

        for(Iterator iter=shaderMap.values().iterator(); iter.hasNext(); ) {
            ShaderCode shaderCode = (ShaderCode) iter.next();
            if(!shaderCode.compile(gl, verboseOut)) {
                return false;
            }
            ShaderUtil.attachShader(gl, shaderProgram, shaderCode.shader());
        }

        // Link the program
        gl.glLinkProgram(shaderProgram);

        programLinked = ShaderUtil.isProgramValid(gl, shaderProgram, System.err);

        return programLinked;
    }

    public boolean equals(Object obj) {
        if(this == obj)  { return true; }
        if(obj instanceof ShaderProgram) {
            return id()==((ShaderProgram)obj).id();
        }
        return false;
    }

    public int hashCode() {
        return id.intValue();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("ShaderProgram[id="+id);
        buf.append(", linked="+programLinked+", inUse="+programInUse+", program: "+shaderProgram+", [");
        for(Iterator iter=shaderMap.values().iterator(); iter.hasNext(); ) {
            buf.append((ShaderCode) iter.next());
            buf.append(" ");
        }
        buf.append("]");
        return buf.toString();
    }

    protected synchronized void glUseProgram(GL2ES2 gl, boolean on) {
        if(!programLinked) throw new GLException("Program is not linked");
        if(programInUse==on) return;
        gl.glUseProgram(on?shaderProgram:0);
        programInUse = on;

        //Throwable tX = new Throwable("Info: ShaderProgram.glUseProgram: "+on);
        //tX.printStackTrace();

    }

    protected boolean programLinked = false;
    protected boolean programInUse = false;
    protected int shaderProgram=-1;
    protected HashMap shaderMap = new HashMap();
    protected Integer    id = null;

    private static synchronized Integer getNextID() {
        return new Integer(nextID++);
    }
    protected static int nextID = 1;
}

