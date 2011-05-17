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

import com.jogamp.common.os.Platform;

import java.util.HashSet;
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
     */
    public int        id() { return id; }

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
        useProgram(gl, false);
        for(Iterator<ShaderCode> iter=allShaderCode.iterator(); iter.hasNext(); ) {
            ShaderCode shaderCode = iter.next();
            if(attachedShaderCode.remove(shaderCode)) {
                ShaderUtil.detachShader(gl, shaderProgram, shaderCode.shader());
            }
            if(releaseShaderToo) {
                shaderCode.destroy(gl);
            }
        }
        allShaderCode.clear();
        attachedShaderCode.clear();
        gl.glDeleteProgram(shaderProgram);
        shaderProgram=-1;
    }

    //
    // ShaderCode handling
    //

    /**
     * Adds a new shader to this program.
     * 
     * <p>This command does not compile and attach the shader,
     * use {@link #add(GL2ES2, ShaderCode)} for this purpose.</p>
     */
    public synchronized void add(ShaderCode shaderCode) throws GLException {
        allShaderCode.add(shaderCode);
    }

    public synchronized boolean contains(ShaderCode shaderCode) {
        return allShaderCode.contains(shaderCode);
    }
    
    /**
     * Warning slow O(n) operation ..
     * @param id
     * @return
     */
    public synchronized ShaderCode getShader(int id) {
        for(Iterator<ShaderCode> iter=allShaderCode.iterator(); iter.hasNext(); ) {
            ShaderCode shaderCode = iter.next();
            if(shaderCode.id() == id) {
                return shaderCode;
            }
        }
        return null;
    }

    //
    // ShaderCode / Program handling
    //

    /**
     * Creates the empty GL program object using {@link GL2ES2#glCreateProgram()}
     *  
     * @param gl
     */
    public synchronized final void init(GL2ES2 gl) {
        if(0>shaderProgram) {
            shaderProgram = gl.glCreateProgram();
        }
    }
    
    /**
     * Adds a new shader to a this non running program.
     *
     * <p>Compiles and attaches the shader, if not done yet.</p>
     * 
     * @return true if the shader was successfully added, false if compilation failed.
     */
    public synchronized boolean add(GL2ES2 gl, ShaderCode shaderCode, PrintStream verboseOut) {
        init(gl);
        if( allShaderCode.add(shaderCode) ) {
            if(!shaderCode.compile(gl, verboseOut)) {
                return false;
            }
            if(attachedShaderCode.add(shaderCode)) {
                ShaderUtil.attachShader(gl, shaderProgram, shaderCode.shader());
            }
        }
        return true;
    }

    /**
     * Replace a shader in a program and re-links the program.
     *
     * @param gl 
     * @param oldShader   the to be replace Shader
     * @param newShader   the new ShaderCode
     * @param verboseOut  the optional verbose output stream
     * 
     * @return true if all steps are valid, shader compilation, attachment and linking; otherwise false.
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
    public synchronized boolean replaceShader(GL2ES2 gl, ShaderCode oldShader, ShaderCode newShader, PrintStream verboseOut) {
        init(gl);
        
        if(!newShader.compile(gl, verboseOut)) {
            return false;
        }
        
        boolean shaderWasInUse = inUse();
        if(shaderWasInUse) {
            useProgram(gl, false);
        }
        
        if(null != oldShader && allShaderCode.remove(oldShader)) {
            if(attachedShaderCode.remove(oldShader)) {
                ShaderUtil.detachShader(gl, shaderProgram, oldShader.shader());
            }
        }
        
        add(newShader);
        if(attachedShaderCode.add(newShader)) {
            ShaderUtil.attachShader(gl, shaderProgram, newShader.shader());
        }
        
        gl.glLinkProgram(shaderProgram);
        
        programLinked = ShaderUtil.isProgramValid(gl, shaderProgram, System.err);
        if ( programLinked && shaderWasInUse )  {
            useProgram(gl, true);
        }
        return programLinked;
    }

    /**
     * Links the shader code to the program.
     * 
     * <p>Compiles and attaches the shader code to the program if not done by yet</p>
     * 
     * <p>Within this process, all GL resources (shader and program objects) are created if necessary.</p>
     *  
     * @param gl
     * @param verboseOut
     * @return
     * 
     * @see #init(GL2ES2)
     */
    public synchronized boolean link(GL2ES2 gl, PrintStream verboseOut) {
        init(gl);

        for(Iterator<ShaderCode> iter=allShaderCode.iterator(); iter.hasNext(); ) {
            final ShaderCode shaderCode = iter.next();
            if(!shaderCode.compile(gl, verboseOut)) {
                return false;
            }
            if(attachedShaderCode.add(shaderCode)) {
                ShaderUtil.attachShader(gl, shaderProgram, shaderCode.shader());
            }
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
        return id;
    }

    public StringBuilder toString(StringBuilder sb) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        sb.append("ShaderProgram[id=").append(id);
        sb.append(", linked="+programLinked+", inUse="+programInUse+", program: "+shaderProgram+",");
        for(Iterator<ShaderCode> iter=allShaderCode.iterator(); iter.hasNext(); ) {
            sb.append(Platform.getNewline()).append("   ").append(iter.next());
        }
        sb.append("]");
        return sb;
    }
        
    public String toString() {
        return toString(null).toString();
    }

    public synchronized void useProgram(GL2ES2 gl, boolean on) {
        if(!programLinked) throw new GLException("Program is not linked");
        if(programInUse==on) return;
        gl.glUseProgram(on?shaderProgram:0);
        programInUse = on;
    }

    protected boolean programLinked = false;
    protected boolean programInUse = false;
    protected int shaderProgram=-1;
    protected HashSet<ShaderCode> allShaderCode = new HashSet<ShaderCode>();
    protected HashSet<ShaderCode> attachedShaderCode = new HashSet<ShaderCode>();
    protected int id = -1;

    private static synchronized int getNextID() {
        return nextID++;
    }
    protected static int nextID = 1;
}

