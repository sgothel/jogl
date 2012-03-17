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
import com.jogamp.common.util.IOUtil;

import javax.media.opengl.*;

import jogamp.opengl.Debug;

import java.util.*;
import java.nio.*;
import java.io.*;
import java.net.*;

/**
 * Convenient shader code class to use and instantiate vertex or fragment programs.
 * <p>
 * A documented example of how to use this code is available
 * {@link #create(GL2ES2, int, int, Class, String, String, String) here} and
 * {@link #create(GL2ES2, int, int, Class, String, String[], String, String) here}.
 * </p>  
 */
public class ShaderCode {
    public static final boolean DEBUG = Debug.debug("GLSLCode");
    public static final boolean DEBUG_CODE = Debug.isPropertyDefined("jogl.debug.GLSLCode", true);

    /** Unique resource suffix for {@link GL2ES2#GL_VERTEX_SHADER} in source code: <code>vp</code> */
    public static final String SUFFIX_VERTEX_SOURCE   =  "vp" ;
    
    /** Unique resource suffix for {@link GL2ES2#GL_VERTEX_SHADER} in binary: <code>bvp</code> */
    public static final String SUFFIX_VERTEX_BINARY   = "bvp" ;
    
    /** Unique resource suffix for {@link GL2ES2#GL_FRAGMENT_SHADER} in source code: <code>fp</code> */
    public static final String SUFFIX_FRAGMENT_SOURCE =  "fp" ;
    
    /** Unique resource suffix for {@link GL2ES2#GL_FRAGMENT_SHADER} in binary: <code>bfp</code> */
    public static final String SUFFIX_FRAGMENT_BINARY = "bfp" ;
    
    /** Unique relative path for binary shader resources for {@link GLES2#GL_NVIDIA_PLATFORM_BINARY_NV NVIDIA}: <code>nvidia</code> */
    public static final String SUB_PATH_NVIDIA = "nvidia" ;

    /**
     * @param type either {@link GL2ES2#GL_VERTEX_SHADER} or {@link GL2ES2#GL_FRAGMENT_SHADER}
     * @param count number of shaders
     * @param source string array containing the shader sources, organized as <code>source[count][strings-per-shader]</code>
     * 
     * @throws IllegalArgumentException if <code>count</count> and <code>source.length</code> do not match
     */
    public ShaderCode(int type, int count, String[][] source) {
        if(source.length != count) {
            throw new IllegalArgumentException("shader number ("+count+") and sourceFiles array ("+source.length+") of different lenght.");
        }
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
        shader = Buffers.newDirectIntBuffer(count);
        id = getNextID();

        if(DEBUG_CODE) {
            System.out.println("Created: "+toString());
            // dumpShaderSource(System.out); // already done in readShaderSource
        }
    }

    /**
     * @param type either {@link GL2ES2#GL_VERTEX_SHADER} or {@link GL2ES2#GL_FRAGMENT_SHADER}
     * @param count number of shaders
     * @param binary binary buffer containing the shader binaries, 
     */
    public ShaderCode(int type, int count, int binFormat, Buffer binary) {
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
        shader = Buffers.newDirectIntBuffer(count);
        id = getNextID();
    }

    /**
     * Creates a complete {@link ShaderCode} object while reading all shader source of <code>sourceFiles</code>,
     * which location is resolved using the <code>context</code> class, see {@link #readShaderSource(Class, String)}.
     * 
     * @param gl current GL object to determine whether a shader compiler is available. If null, no validation is performed.
     * @param type either {@link GL2ES2#GL_VERTEX_SHADER} or {@link GL2ES2#GL_FRAGMENT_SHADER}
     * @param count number of shaders
     * @param context class used to help resolving the source location
     * @param sourceFiles array of source locations, organized as <code>sourceFiles[count]</code>
     * 
     * @throws IllegalArgumentException if <code>count</count> and <code>sourceFiles.length</code> do not match
     * @see #readShaderSource(Class, String)
     */
    public static ShaderCode create(GL2ES2 gl, int type, int count, Class<?> context, String[] sourceFiles) {
        if(null != gl && !ShaderUtil.isShaderCompilerAvailable(gl)) {
            return null;
        }

        String[][] shaderSources = null;
        if(null!=sourceFiles) {
            // sourceFiles.length and count is validated in ctor
            shaderSources = new String[sourceFiles.length][1];
            for(int i=0; i<sourceFiles.length; i++) {
                try {
                    shaderSources[i][0] = readShaderSource(context, sourceFiles[i]);
                } catch (IOException ioe) {
                    throw new RuntimeException("readShaderSource("+sourceFiles[i]+") error: ", ioe);
                }
                if(null == shaderSources[i][0]) {
                    shaderSources = null;
                }
            }
        }
        if(null==shaderSources) {
            return null;
        }
        return new ShaderCode(type, count, shaderSources);
    }

    /**
     * Creates a complete {@link ShaderCode} object while reading the shader binary of <code>binaryFile</code>,
     * which location is resolved using the <code>context</code> class, see {@link #readShaderBinary(Class, String)}.
     * 
     * @param type either {@link GL2ES2#GL_VERTEX_SHADER} or {@link GL2ES2#GL_FRAGMENT_SHADER}
     * @param count number of shaders
     * @param context class used to help resolving the source location
     * @param binFormat a valid native binary format as they can be queried by {@link ShaderUtil#getShaderBinaryFormats(GL)}.
     * @param sourceFiles array of source locations, organized as <code>sourceFiles[count]</code>
     * 
     * @see #readShaderBinary(Class, String)
     * @see ShaderUtil#getShaderBinaryFormats(GL)
     */    
    public static ShaderCode create(int type, int count, Class<?> context, int binFormat, String binaryFile) {
        ByteBuffer shaderBinary = null;
        if(null!=binaryFile && 0<=binFormat) {
            try {
                shaderBinary = readShaderBinary(context, binaryFile);
            } catch (IOException ioe) {
                throw new RuntimeException("readShaderBinary("+binaryFile+") error: ", ioe);
            }
            if(null == shaderBinary) {
                binFormat = -1;
            }
        }
        if(null==shaderBinary) {
            return null;
        }
        return new ShaderCode(type, count, binFormat, shaderBinary);
    }

    /**
     * Returns a unique suffix for shader resources as follows:
     * <ul>
     *   <li>Source<ul>
     *     <li>{@link GL2ES2#GL_VERTEX_SHADER vertex}: {@link #SUFFIX_VERTEX_SOURCE}</li>
     *     <li>{@link GL2ES2#GL_FRAGMENT_SHADER fragment}: {@link #SUFFIX_FRAGMENT_SOURCE}</li></ul></li>
     *   <li>Binary<ul>
     *     <li>{@link GL2ES2#GL_VERTEX_SHADER vertex}: {@link #SUFFIX_VERTEX_BINARY}</li>
     *     <li>{@link GL2ES2#GL_FRAGMENT_SHADER fragment}: {@link #SUFFIX_FRAGMENT_BINARY}</li></ul></li>
     * </ul> 
     * @param binary true for a binary resource, false for a source resource 
     * @param type either {@link GL2ES2#GL_VERTEX_SHADER} or {@link GL2ES2#GL_FRAGMENT_SHADER}
     * 
     * @throws GLException if <code>type</code> is not supported
     * 
     * @see #create(GL2ES2, int, int, Class, String, String, String)
     */
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

    /** 
     * Returns a unique relative path for binary shader resources as follows:
     * <ul>
     *   <li>{@link GLES2#GL_NVIDIA_PLATFORM_BINARY_NV NVIDIA}: {@link #SUB_PATH_NVIDIA}</li>
     * </ul>
     * 
     * @throws GLException if <code>binFormat</code> is not supported
     * 
     * @see #create(GL2ES2, int, int, Class, String, String, String)
     */    
    public static String getBinarySubPath(int binFormat) {
        switch (binFormat) {
            case GLES2.GL_NVIDIA_PLATFORM_BINARY_NV:
                return SUB_PATH_NVIDIA;
            default:
                throw new GLException("unsupported binary format: "+binFormat);
        }
    }

    /**
     * Convenient creation method for instantiating a complete {@link ShaderCode} object 
     * either from source code using {@link #create(GL2ES2, int, int, Class, String[])}, 
     * or from a binary code using {@link #create(int, int, Class, int, String)},
     * whatever is available first.
     * <p>
     * The source and binary location names are expected w/o suffixes which are 
     * resolved and appended using {@link #getFileSuffix(boolean, int)}.
     * </p>
     * <p>
     * Additionally, the binary resource is expected within a subfolder of <code>binRoot</code>
     * which reflects the vendor specific binary format, see {@link #getBinarySubPath(int)}.
     * All {@link ShaderUtil#getShaderBinaryFormats(GL)} are being iterated
     * using the binary subfolder, the first existing resource is being used. 
     * </p>
     * 
     * Example:
     * <pre>
     *   Your std JVM layout (plain or within a JAR):
     *   
     *      org/test/glsl/MyShaderTest.class
     *      org/test/glsl/shader/vertex.vp
     *      org/test/glsl/shader/fragment.fp
     *      org/test/glsl/shader/bin/nvidia/vertex.bvp
     *      org/test/glsl/shader/bin/nvidia/fragment.bfp
     *      
     *   Your Android APK layout:
     *   
     *      classes.dex
     *      assets/org/test/glsl/shader/vertex.vp
     *      assets/org/test/glsl/shader/fragment.fp
     *      assets/org/test/glsl/shader/bin/nvidia/vertex.bvp
     *      assets/org/test/glsl/shader/bin/nvidia/fragment.bfp
     *      ...
     *   
     *   Your invocation in org/test/glsl/MyShaderTest.java:
     *   
     *      ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, this.getClass(),
     *                                         "shader", new String[] { "vertex" }, "shader/bin", "vertex");
     *      ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, this.getClass(),
     *                                         "shader", new String[] { "vertex" }, "shader/bin", "fragment");
     *      ShaderProgram sp0 = new ShaderProgram();
     *      sp0.add(gl, vp0, System.err);
     *      sp0.add(gl, fp0, System.err);
     *      st.attachShaderProgram(gl, sp0, true);
     * </pre>
     * A simplified entry point is {@link #create(GL2ES2, int, int, Class, String, String, String)}.
     * 
     * <p>
     * The location is finally being resolved using the <code>context</code> class, see {@link #readShaderBinary(Class, String)}.
     * </p>
     * 
     * @param gl current GL object to determine whether a shader compiler is available (if <code>source</code> is used),
     *           or to determine the shader binary format (if <code>binary</code> is used).
     * @param type either {@link GL2ES2#GL_VERTEX_SHADER} or {@link GL2ES2#GL_FRAGMENT_SHADER}
     * @param count number of shaders
     * @param context class used to help resolving the source and binary location
     * @param srcRoot relative <i>root</i> path for <code>srcBasenames</code>
     * @param srcBasenames basenames w/o path or suffix relative to <code>srcRoot</code> for the shader's source code
     * @param binRoot relative <i>root</i> path for <code>binBasenames</code>
     * @param binBasename basename w/o path or suffix relative to <code>binRoot</code> for the shader's binary code
     * 
     * @throws IllegalArgumentException if <code>count</count> and <code>srcBasenames.length</code> do not match
     * 
     * @see #create(GL2ES2, int, int, Class, String[])
     * @see #create(int, int, Class, int, String)
     * @see #readShaderSource(Class, String)
     * @see #getFileSuffix(boolean, int)
     * @see ShaderUtil#getShaderBinaryFormats(GL)
     * @see #getBinarySubPath(int)
     */    
    public static ShaderCode create(GL2ES2 gl, int type, int count, Class<?> context, 
                                    String srcRoot, String[] srcBasenames, String binRoot, String binBasename) {
        ShaderCode res = null;
        final String srcPath[];
        String srcPathString = null;
        String binFileName = null;

        if(null!=srcRoot && null!=srcBasenames && ShaderUtil.isShaderCompilerAvailable(gl)) {
            srcPath = new String[srcBasenames.length];
            final String srcSuffix = getFileSuffix(false, type);
            for(int i=0; i<srcPath.length; i++) {
                srcPath[i] = srcRoot + '/' + srcBasenames[i] + "." + srcSuffix;
            }
            res = create(gl, type, count, context, srcPath);
            if(null!=res) {
                return res;
            }
            srcPathString = Arrays.toString(srcPath);
        } else {
            srcPath = null;
        }
        if(null!=binRoot && null!=binBasename) {
            Set<Integer> binFmts = ShaderUtil.getShaderBinaryFormats(gl);
            final String binSuffix = getFileSuffix(true, type);
            for(Iterator<Integer> iter=binFmts.iterator(); iter.hasNext(); ) {
                int bFmt = iter.next().intValue();
                String bFmtPath = getBinarySubPath(bFmt);
                if(null==bFmtPath) continue;
                binFileName = binRoot + '/' + bFmtPath + '/' + binBasename + "." + binSuffix;
                res = create(type, count, context, bFmt, binFileName);
                if(null!=res) {
                    return res;
                }
            }
        }
        throw new GLException("No shader code found (source nor binary) for src: "+srcPathString+
                              ", bin: "+binFileName);
    }

    /**
     * Simplified variation of {@link #create(GL2ES2, int, int, Class, String, String[], String, String)}.
     * <br>
     * 
     * Example:
     * <pre>
     *   Your std JVM layout (plain or within a JAR):
     *   
     *      org/test/glsl/MyShaderTest.class
     *      org/test/glsl/shader/vertex.vp
     *      org/test/glsl/shader/fragment.fp
     *      org/test/glsl/shader/bin/nvidia/vertex.bvp
     *      org/test/glsl/shader/bin/nvidia/fragment.bfp
     *      
     *   Your Android APK layout:
     *   
     *      classes.dex
     *      assets/org/test/glsl/shader/vertex.vp
     *      assets/org/test/glsl/shader/fragment.fp
     *      assets/org/test/glsl/shader/bin/nvidia/vertex.bvp
     *      assets/org/test/glsl/shader/bin/nvidia/fragment.bfp
     *      ...
     *   
     *   Your invocation in org/test/glsl/MyShaderTest.java:
     *   
     *      ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, this.getClass(),
     *                                         "shader", "shader/bin", "vertex");
     *      ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, this.getClass(),
     *                                         "shader", "shader/bin", "fragment");
     *      ShaderProgram sp0 = new ShaderProgram();
     *      sp0.add(gl, vp0, System.err);
     *      sp0.add(gl, fp0, System.err);
     *      st.attachShaderProgram(gl, sp0, true);
     * </pre>
     * 
     * @param gl current GL object to determine whether a shader compiler is available (if <code>source</code> is used),
     *           or to determine the shader binary format (if <code>binary</code> is used).
     * @param type either {@link GL2ES2#GL_VERTEX_SHADER} or {@link GL2ES2#GL_FRAGMENT_SHADER}
     * @param count number of shaders, must be one
     * @param context class used to help resolving the source and binary location
     * @param srcRoot relative <i>root</i> path for <code>basename</code>
     * @param binRoot relative <i>root</i> path for <code>basename</code>
     * @param basenames basename w/o path or suffix relative to <code>srcRoot</code> and <code>binRoot</code>
     *                  for the shader's source and binary code.
     * 
     * @throws IllegalArgumentException if <code>count</count> is not 1
     * 
     * @see #create(GL2ES2, int, int, Class, String, String[], String, String)
     */    
    public static ShaderCode create(GL2ES2 gl, int type, int count, Class<?> context, 
                                    String srcRoot, String binRoot, String basename) {
        return create(gl, type, count, context, srcRoot, new String[] { basename }, binRoot, basename );        
    }
    
    /**
     * returns the uniq shader id as an integer
     */
    public int id() { return id; }

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
        id=-1;
    }

    public boolean equals(Object obj) {
        if(this==obj) { return true; }
        if(obj instanceof ShaderCode) {
            return id()==((ShaderCode)obj).id();
        }
        return false;
    }
    public int hashCode() {
        return id;
    }
    public String toString() {
        StringBuffer buf = new StringBuffer("ShaderCode[id="+id+", type="+shaderTypeStr()+", valid="+valid+", shader: ");
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

    private static int readShaderSource(Class<?> context, URLConnection conn, StringBuffer result, int lineno) throws IOException  {        
        if(DEBUG_CODE) {
            System.err.printf("%3d: // %s\n", lineno, conn.getURL());
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        try {
            String line = null;
            while ((line = reader.readLine()) != null) {
                lineno++;
                if(DEBUG_CODE) {
                    System.err.printf("%3d: %s\n", lineno, line);
                }
                if (line.startsWith("#include ")) {
                    String includeFile = line.substring(9).trim();
                    URLConnection nextConn = null;
                    
                    // Try relative of current shader location
                    nextConn = IOUtil.openURL(IOUtil.getRelativeOf(conn.getURL(), includeFile), "ShaderCode.relativeOf ");
                    if (nextConn == null) {
                        // Try relative of class and absolute
                        nextConn = IOUtil.getResource(context, includeFile);        
                    }
                    if (nextConn == null) {
                        // Fail
                        throw new FileNotFoundException("Can't find include file " + includeFile);
                    }
                    lineno = readShaderSource(context, nextConn, result, lineno);
                } else {
                    result.append(line + "\n");
                }
            }
        } finally {
            IOUtil.close(reader, false);
        }
        return lineno;
    }

    /**
     * 
     * @param context
     * @param conn
     * @param result
     * @throws IOException
     */
    public static void readShaderSource(Class<?> context, URLConnection conn, StringBuffer result) throws IOException {
        if(DEBUG_CODE) {
            System.err.println();
            System.err.println("// -----------------------------------------------------------");
        }
        readShaderSource(context, conn, result, 0);
        if(DEBUG_CODE) {
            System.err.println("// -----------------------------------------------------------");
            System.err.println();
        }
    }
    
    /**
     * Reads shader source located in <code>path</code>,
     * either relative to the <code>context</code> class or absolute <i>as-is</i>.
     * <p>
     * Final location lookup is perfomed via {@link ClassLoader#getResource(String)} and {@link ClassLoader#getSystemResource(String)},
     * see {@link IOUtil#getResource(Class, String)}.
     * </p>
     *  
     * @param context class used to help resolve the source location
     * @param path location of shader source
     * @throws IOException 
     * 
     * @see IOUtil#getResource(Class, String)
     */    
    public static String readShaderSource(Class<?> context, String path) throws IOException {
        URLConnection conn = IOUtil.getResource(context, path);        
        if (conn == null) {
            return null;
        }
        StringBuffer result = new StringBuffer();
        readShaderSource(context, conn, result);
        return result.toString();
    }

    /**
     * Reads shader binary located in <code>path</code>, 
     * either relative to the <code>context</code> class or absolute <i>as-is</i>.
     * <p>
     * Final location lookup is perfomed via {@link ClassLoader#getResource(String)} and {@link ClassLoader#getSystemResource(String)},
     * see {@link IOUtil#getResource(Class, String)}.
     * </p>
     *  
     * @param context class used to help resolve the source location
     * @param path location of shader binary
     * @throws IOException 
     * 
     * @see IOUtil#getResource(Class, String)
     */
    public static ByteBuffer readShaderBinary(Class<?> context, String path) throws IOException {
        final URLConnection conn = IOUtil.getResource(context, path);
        if (conn == null) {
            return null;
        }
        final BufferedInputStream bis = new BufferedInputStream( conn.getInputStream() );
        try {
            return IOUtil.copyStream2ByteBuffer( bis );
        } finally {
            IOUtil.close(bis, false);
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
    protected int        id = -1;

    protected boolean valid=false;

    private static synchronized int getNextID() {
        return nextID++;
    }
    protected static int nextID = 1;
}

