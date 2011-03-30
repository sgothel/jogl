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

import com.jogamp.common.nio.Buffers;
import javax.media.opengl.*;
import com.jogamp.opengl.util.*;
import jogamp.opengl.Debug;

import java.util.*;
import java.nio.*;
import java.io.*;
import java.net.*;
import java.security.*;

public class ShaderCode {
    public static final boolean DEBUG = Debug.debug("GLSLCode");
    public static final boolean DEBUG_CODE = Debug.isPropertyDefined("jogl.debug.GLSLCode", true, AccessController.getContext());

    public static final String SUFFIX_VERTEX_SOURCE   =  "vp" ;
    public static final String SUFFIX_VERTEX_BINARY   = "bvp" ;
    public static final String SUFFIX_FRAGMENT_SOURCE =  "fp" ;
    public static final String SUFFIX_FRAGMENT_BINARY = "bfp" ;
    
    public static final String SUB_PATH_NVIDIA = "nvidia" ;

    public ShaderCode(int type, int number, String[][] source) {
        switch (type) {
            case GL2ES2.GL_VERTEX_SHADER:
            case GL2ES2.GL_FRAGMENT_SHADER:
                break;
            default:
                throw new GLException("Unknown shader type: "+type);
        }
        shaderSource = source;
        shaderBinaryFormat = -1;
        shaderBinary = null;
        shaderType   = type;
        shader = Buffers.newDirectIntBuffer(number);
        id = getNextID();

        if(DEBUG_CODE) {
            System.out.println("Created: "+toString());
            dumpShaderSource(System.out);
        }
    }

    public ShaderCode(int type, int number, int binFormat, Buffer binary) {
        switch (type) {
            case GL2ES2.GL_VERTEX_SHADER:
            case GL2ES2.GL_FRAGMENT_SHADER:
                break;
            default:
                throw new GLException("Unknown shader type: "+type);
        }
        shaderSource = null;
        shaderBinaryFormat = binFormat;
        shaderBinary = binary;
        shaderType   = type;
        shader = Buffers.newDirectIntBuffer(number);
        id = getNextID();
    }

    public static ShaderCode create(GL2ES2 gl, int type, int number, Class context, String[] sourceFiles) {
        if(!ShaderUtil.isShaderCompilerAvailable(gl)) return null;

        String[][] shaderSources = null;
        if(null!=sourceFiles) {
            shaderSources = new String[sourceFiles.length][1];
            for(int i=0; null!=shaderSources && i<sourceFiles.length; i++) {
                shaderSources[i][0] = readShaderSource(context, sourceFiles[i]);
                if(null == shaderSources[i][0]) {
                    shaderSources = null;
                }
            }
        }
        if(null==shaderSources) {
            return null;
        }
        return new ShaderCode(type, number, shaderSources);
    }

    public static ShaderCode create(int type, int number, Class context, int binFormat, String binaryFile) {
        ByteBuffer shaderBinary = null;
        if(null!=binaryFile && 0<=binFormat) {
            shaderBinary = readShaderBinary(context, binaryFile);
            if(null == shaderBinary) {
                binFormat = -1;
            }
        }
        if(null==shaderBinary) {
            return null;
        }
        return new ShaderCode(type, number, binFormat, shaderBinary);
    }

    public static String getFileSuffix(boolean binary, int type) {
        switch (type) {
            case GL2ES2.GL_VERTEX_SHADER:
                return binary?SUFFIX_VERTEX_BINARY:SUFFIX_VERTEX_SOURCE;
            case GL2ES2.GL_FRAGMENT_SHADER:
                return binary?SUFFIX_FRAGMENT_BINARY:SUFFIX_FRAGMENT_SOURCE;
            default:
                throw new GLException("illegal shader type: "+type);
        }
    }

    public static String getBinarySubPath(int binFormat) {
        switch (binFormat) {
            case GLES2.GL_NVIDIA_PLATFORM_BINARY_NV:
                return SUB_PATH_NVIDIA;
            default:
                throw new GLException("unsupported binary format: "+binFormat);
        }
    }

    public static ShaderCode create(GL2ES2 gl, int type, int number, Class context, 
                                    String srcRoot, String binRoot, String basename) {
        ShaderCode res = null;
        String srcFileName = null;
        String binFileName = null;

        if(ShaderUtil.isShaderCompilerAvailable(gl)) {
            String srcPath[] = new String[1];
            srcFileName = srcRoot + '/' + basename + "." + getFileSuffix(false, type);
            srcPath[0] = srcFileName;
            res = create(gl, type, number, context, srcPath);
            if(null!=res) {
                return res;
            }
        }
        Set binFmts = ShaderUtil.getShaderBinaryFormats(gl);
        for(Iterator iter=binFmts.iterator(); null==res && iter.hasNext(); ) {
            int bFmt = ((Integer)(iter.next())).intValue();
            String bFmtPath = getBinarySubPath(bFmt);
            if(null==bFmtPath) continue;
            binFileName = binRoot + '/' + bFmtPath + '/' + basename + "." + getFileSuffix(true, type);
            res = create(type, number, context, bFmt, binFileName);
        }

        if(null==res) {
            throw new GLException("No shader code found (source nor binary) for src: "+srcFileName+
                                  ", bin: "+binFileName);
        }

        return res;
    }

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

    public int        shaderType() { return shaderType; }
    public String     shaderTypeStr() { return shaderTypeStr(shaderType); }

    public static String shaderTypeStr(int type) { 
        switch (type) {
            case GL2ES2.GL_VERTEX_SHADER:
                return "VERTEX_SHADER";
            case GL2ES2.GL_FRAGMENT_SHADER:
                return "FRAGMENT_SHADER";
        }
        return "UNKNOWN_SHADER";
    }

    public int        shaderBinaryFormat() { return shaderBinaryFormat; }
    public Buffer     shaderBinary() { return shaderBinary; }
    public String[][] shaderSource() { return shaderSource; }

    public boolean    isValid() { return valid; }

    public IntBuffer  shader() { return shader; }

    public boolean compile(GL2ES2 gl) {
        return compile(gl, null);
    }
    public boolean compile(GL2ES2 gl, PrintStream verboseOut) {
        if(isValid()) return true;

        // Create & Compile the vertex/fragment shader objects
        if(null!=shaderSource) {
            valid=ShaderUtil.createAndCompileShader(gl, shader, shaderType,
                                                    shaderSource, verboseOut);
        } else if(null!=shaderBinary) {
            valid=ShaderUtil.createAndLoadShader(gl, shader, shaderType,
                                                 shaderBinaryFormat, shaderBinary, verboseOut);
        } else {
            throw new GLException("no code (source or binary)");
        }
        return valid;
    }

    public void destroy(GL2ES2 gl) {
        if(isValid()) {
            if(null!=gl) {
                ShaderUtil.deleteShader(gl, shader());
            }
            valid=false;
        }
        if(null!=shaderBinary) {
            shaderBinary.clear();
            shaderBinary=null;
        }
        shaderSource=null;
        shaderBinaryFormat=-1;
        shaderType=-1;
        id=null;
    }

    public boolean equals(Object obj) {
        if(this==obj) { return true; }
        if(obj instanceof ShaderCode) {
            return id()==((ShaderCode)obj).id();
        }
        return false;
    }
    public int hashCode() {
        return id.intValue();
    }
    public String toString() {
        StringBuffer buf = new StringBuffer("ShaderCode [id="+id+", type="+shaderTypeStr()+", valid="+valid+", shader: ");
        for(int i=0; i<shader.remaining(); i++) {
            buf.append(" "+shader.get(i));
        }
        if(null!=shaderSource) {
            buf.append(", source]");
        } else if(null!=shaderBinary) {
            buf.append(", binary "+shaderBinary+"]");
        }
        return buf.toString();
    }

    public void dumpShaderSource(PrintStream out) {
        if(null==shaderSource) {
            out.println("<no shader source>");
            return;
        }
        int sourceNum = (null!=shaderSource)?shaderSource.length:0;
        int shaderNum = (null!=shader)?shader.capacity():0;
        for(int i=0; i<shaderNum; i++) {
            out.println("");
            out.println("Shader #"+i+"/"+shaderNum+" name "+shader.get(i));
            out.println("--------------------------------------------------------------");
            if(i>=sourceNum) {
                out.println("<no shader source>");
            } else {
                String[] src = shaderSource[i];
                for(int j=0; j<src.length; j++) {
                    out.println("Segment "+j+"/"+src.length+" :");
                    out.println(src[j]);
                    out.println("");
                }
            }
            out.println("--------------------------------------------------------------");
        }
    }

    public static void readShaderSource(ClassLoader context, String path, URL url, StringBuffer result) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#include ")) {
                    String includeFile = line.substring(9).trim();
                    // Try relative path first
                    String next = Locator.getRelativeOf(path, includeFile);
                    URL nextURL = Locator.getResource(next, context);
                    if (nextURL == null) {
                        // Try absolute path
                        next = includeFile;
                        nextURL = Locator.getResource(next, context);
                    }
                    if (nextURL == null) {
                        // Fail
                        throw new FileNotFoundException("Can't find include file " + includeFile);
                    }
                    readShaderSource(context, next, nextURL, result);
                } else {
                    result.append(line + "\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readShaderSource(Class context, String path) {
        ClassLoader contextCL = (null!=context)?context.getClassLoader():null;
        URL url = Locator.getResource(context, path);        
        if (url == null) {
            return null;
        }        
        File pf = new File(url.getPath());
        path = pf.getParent() + "/" ;
        
        StringBuffer result = new StringBuffer();
        readShaderSource(contextCL, path, url, result);
        return result.toString();
    }

    public static ByteBuffer readShaderBinary(Class context, String path) {
        try {
            URL url = Locator.getResource(context, path);
            if (url == null) {
                return null;
            }
            return StreamUtil.readAll2Buffer(new BufferedInputStream(url.openStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //----------------------------------------------------------------------
    // Internals only below this point
    //

    protected String[][] shaderSource = null;
    protected Buffer     shaderBinary = null;
    protected int        shaderBinaryFormat = -1;
    protected IntBuffer  shader = null;
    protected int        shaderType = -1;
    protected Integer    id = null;

    protected boolean valid=false;

    private static synchronized Integer getNextID() {
        return new Integer(nextID++);
    }
    protected static int nextID = 1;
}

