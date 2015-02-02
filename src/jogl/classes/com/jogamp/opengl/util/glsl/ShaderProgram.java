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

import com.jogamp.opengl.*;

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

    /** Returns the shader program name, which is non zero if valid. */
    public int program() { return shaderProgram; }

    /**
     * returns the uniq shader id as an integer
     */
    public int id() { return id; }

    /**
     * Detaches all shader codes and deletes the program.
     * Destroys the shader codes as well.
     * Calls release(gl, true)
     *
     * @see #release(GL2ES2, boolean)
     */
    public synchronized void destroy(final GL2ES2 gl) {
        release(gl, true);
    }

    /**
     * Detaches all shader codes and deletes the program,
     * but leaves the shader code intact.
     * Calls release(gl, false)
     *
     * @see #release(GL2ES2, boolean)
     */
    public synchronized void release(final GL2ES2 gl) {
        release(gl, false);
    }

    /**
     * Detaches all shader codes and deletes the program.
     * If <code>destroyShaderCode</code> is true it destroys the shader codes as well.
     */
    public synchronized void release(final GL2ES2 gl, final boolean destroyShaderCode) {
        if( programLinked ) {
            useProgram(gl, false);
        }
        for(final Iterator<ShaderCode> iter=allShaderCode.iterator(); iter.hasNext(); ) {
            final ShaderCode shaderCode = iter.next();
            if(attachedShaderCode.remove(shaderCode)) {
                ShaderUtil.detachShader(gl, shaderProgram, shaderCode.shader());
            }
            if(destroyShaderCode) {
                shaderCode.destroy(gl);
            }
        }
        allShaderCode.clear();
        attachedShaderCode.clear();
        if( 0 != shaderProgram ) {
            gl.glDeleteProgram(shaderProgram);
            shaderProgram=0;
        }
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
    public synchronized void add(final ShaderCode shaderCode) throws GLException {
        allShaderCode.add(shaderCode);
    }

    public synchronized boolean contains(final ShaderCode shaderCode) {
        return allShaderCode.contains(shaderCode);
    }

    /**
     * Warning slow O(n) operation ..
     * @param id
     * @return
     */
    public synchronized ShaderCode getShader(final int id) {
        for(final Iterator<ShaderCode> iter=allShaderCode.iterator(); iter.hasNext(); ) {
            final ShaderCode shaderCode = iter.next();
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
     * Creates the empty GL program object using {@link GL2ES2#glCreateProgram()},
     * if not already created.
     *
     * @param gl
     * @return true if shader program is valid, i.e. not zero
     */
    public synchronized final boolean init(final GL2ES2 gl) {
        if( 0 == shaderProgram ) {
            shaderProgram = gl.glCreateProgram();
        }
        return 0 != shaderProgram;
    }

    /**
     * Adds a new shader to a this non running program.
     *
     * <p>Compiles and attaches the shader, if not done yet.</p>
     *
     * @return true if the shader was successfully added, false if compilation failed.
     */
    public synchronized boolean add(final GL2ES2 gl, final ShaderCode shaderCode, final PrintStream verboseOut) {
        if( !init(gl) ) { return false; }
        if( allShaderCode.add(shaderCode) ) {
            if( !shaderCode.compile(gl, verboseOut) ) {
                return false;
            }
            if( attachedShaderCode.add(shaderCode) ) {
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
    public synchronized boolean replaceShader(final GL2ES2 gl, final ShaderCode oldShader, final ShaderCode newShader, final PrintStream verboseOut) {
        if(!init(gl) || !newShader.compile(gl, verboseOut)) {
            return false;
        }

        final boolean shaderWasInUse = inUse();
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

        programLinked = ShaderUtil.isProgramLinkStatusValid(gl, shaderProgram, verboseOut);
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
     * @return true if program was successfully linked and is valid, otherwise false
     *
     * @see #init(GL2ES2)
     */
    public synchronized boolean link(final GL2ES2 gl, final PrintStream verboseOut) {
        if( !init(gl) ) {
            programLinked = false; // mark unlinked due to user attempt to [re]link
            return false;
        }

        for(final Iterator<ShaderCode> iter=allShaderCode.iterator(); iter.hasNext(); ) {
            final ShaderCode shaderCode = iter.next();
            if(!shaderCode.compile(gl, verboseOut)) {
                programLinked = false; // mark unlinked due to user attempt to [re]link
                return false;
            }
            if(attachedShaderCode.add(shaderCode)) {
                ShaderUtil.attachShader(gl, shaderProgram, shaderCode.shader());
            }
        }

        // Link the program
        gl.glLinkProgram(shaderProgram);

        programLinked = ShaderUtil.isProgramLinkStatusValid(gl, shaderProgram, verboseOut);

        return programLinked;
    }

    @Override
    public boolean equals(final Object obj) {
        if(this == obj)  { return true; }
        if(obj instanceof ShaderProgram) {
            return id()==((ShaderProgram)obj).id();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public StringBuilder toString(StringBuilder sb) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        sb.append("ShaderProgram[id=").append(id);
        sb.append(", linked="+programLinked+", inUse="+programInUse+", program: "+shaderProgram+",");
        for(final Iterator<ShaderCode> iter=allShaderCode.iterator(); iter.hasNext(); ) {
            sb.append(Platform.getNewline()).append("   ").append(iter.next());
        }
        sb.append("]");
        return sb;
    }

    @Override
    public String toString() {
        return toString(null).toString();
    }

    /**
     * Performs {@link GL2ES2#glValidateProgram(int)} via {@link ShaderUtil#isProgramExecStatusValid(GL, int, PrintStream)}.
     * @see ShaderUtil#isProgramExecStatusValid(GL, int, PrintStream)
     **/
    public synchronized boolean validateProgram(final GL2ES2 gl, final PrintStream verboseOut) {
        return ShaderUtil.isProgramExecStatusValid(gl, shaderProgram, verboseOut);
    }

    public synchronized void useProgram(final GL2ES2 gl, boolean on) {
        if(!programLinked) { throw new GLException("Program is not linked"); }
        if(programInUse==on) { return; }
        if( 0 == shaderProgram ) {
            on = false;
        }
        gl.glUseProgram( on ? shaderProgram : 0 );
        programInUse = on;
    }
    public synchronized void notifyNotInUse() {
        programInUse = false;
    }

    private boolean programLinked = false;
    private boolean programInUse = false;
    private int shaderProgram = 0; // non zero is valid!
    private final HashSet<ShaderCode> allShaderCode = new HashSet<ShaderCode>();
    private final HashSet<ShaderCode> attachedShaderCode = new HashSet<ShaderCode>();
    private final int id;

    private static synchronized int getNextID() {
        return nextID++;
    }
    private static int nextID = 1;
}

