
package javax.media.opengl.util.glsl;

import javax.media.opengl.util.*;
import javax.media.opengl.*;

import java.nio.*;
import java.io.*;
import java.net.*;

public class ShaderCode {
    public ShaderCode(int type, int number, 
                      int binFormat, Buffer binary, String[][] source) {
        switch (type) {
            case GL2ES2.GL_VERTEX_SHADER:
            case GL2ES2.GL_FRAGMENT_SHADER:
                break;
            default:
                throw new GLException("Unknown shader type: "+type);
        }
        shaderSource = source;
        shaderBinaryFormat = binFormat;
        shaderBinary = binary;
        shaderType   = type;
        shader = BufferUtil.newIntBuffer(number);
        id = getNextID();
    }

    public static ShaderCode create(int type, int number, 
                                    Class context, int binFormat, String binaryFile, String[] sourceFiles) {
        String[][] shaderSources = null;
        ByteBuffer shaderBinary = null;
        if(null!=sourceFiles) {
            shaderSources = new String[sourceFiles.length][1];
            for(int i=0; i<sourceFiles.length; i++) {
                shaderSources[i][0] = readShaderSource(context, sourceFiles[i]);
                if(null == shaderSources[i][0]) {
                    throw new RuntimeException("Can't find shader source " + sourceFiles[i]);
                }
            }
        }
        if(null!=binaryFile && 0<=binFormat) {
            shaderBinary = readShaderBinary(context, binaryFile);
            if(null == shaderBinary) {
                System.err.println("Can't find shader binary " + binaryFile + " - ignored");
                binFormat = -1;
            }
        }
        return new ShaderCode(type, number, binFormat, shaderBinary, shaderSources);
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
        valid=gl.glCreateCompileShader(shader, shaderType,
                                       shaderBinaryFormat, shaderBinary,
                                       shaderSource, verboseOut);
        shader.clear();
        return valid;
    }

    public void release(GL2ES2 gl) {
        if(isValid()) {
            gl.glDeleteShader(shader());
            valid=false;
        }
    }

    public boolean equals(Object obj) {
        if(this==obj) return true;
        if(obj instanceof ShaderCode) {
            return id()==((ShaderCode)obj).id();
        }
        return false;
    }
    public int hashCode() {
        return id.intValue();
    }
    public String toString() {
        StringBuffer buf = new StringBuffer("ShaderCode [id="+id+", type="+shaderTypeStr()+", valid="+valid);
        /*
        if(shaderSource!=null) {
            for(int i=0; i<shaderSource.length; i++) {
                for(int j=0; j<shaderSource[i].length; j++) {
                    buf.append("\n\t, ShaderSource["+i+"]["+j+"]:\n");
                    buf.append(shaderSource[i][j]);
                }
            }
        } */
        buf.append("]");
        return buf.toString();
    }

    public static void readShaderSource(ClassLoader context, String path, URL url, StringBuffer result) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#include ")) {
                    String includeFile = line.substring(9).trim();
                    // Try relative path first
                    String next = makeRelative(path, includeFile);
                    URL nextURL = getResource(context, next);
                    if (nextURL == null) {
                        // Try absolute path
                        next = includeFile;
                        nextURL = getResource(context, next);
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
        URL url = getResource(context.getClassLoader(), path);
        if (url == null) {
            // Try again by scoping the path within the class's package
            String className = context.getName().replace('.', '/');
            int lastSlash = className.lastIndexOf('/');
            if (lastSlash >= 0) {
                String tmpPath = className.substring(0, lastSlash + 1) + path;
                url = getResource(context.getClassLoader(), tmpPath);
                if (url != null) {
                    path = tmpPath;
                }
            }
        }
        if (url == null) {
            return null;
        }
        StringBuffer result = new StringBuffer();
        readShaderSource(context.getClassLoader(), path, url, result);
        return result.toString();
    }

    public static ByteBuffer readShaderBinary(Class context, String path) {
        try {
            URL url = getResource(context.getClassLoader(), path);
            if (url == null) {
                // Try again by scoping the path within the class's package
                String className = context.getName().replace('.', '/');
                int lastSlash = className.lastIndexOf('/');
                if (lastSlash >= 0) {
                    String tmpPath = className.substring(0, lastSlash + 1) + path;
                    url = getResource(context.getClassLoader(), tmpPath);
                    if (url != null) {
                        path = tmpPath;
                    }
                }
            }
            if (url == null) {
                return null;
            }
            return readAll(new BufferedInputStream(url.openStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //----------------------------------------------------------------------
    // Internals only below this point
    //

    protected static URL getResource(ClassLoader context, String path) {
        if (context != null) {
            return context.getResource(path);
        } else {
            return ClassLoader.getSystemResource(path);
        }
    }

    protected static String makeRelative(String context, String includeFile) {
        File file = new File(context);
        file = file.getParentFile();
        while (file != null && includeFile.startsWith("../")) {
            file = file.getParentFile();
            includeFile = includeFile.substring(3);
        }
        if (file != null) {
            String res = new File(file, includeFile).getPath();
	    // Handle things on Windows
            return res.replace('\\', '/');
        } else {
            return includeFile;
        }
    }

    private static ByteBuffer readAll(InputStream stream) throws IOException {
        byte[] data = new byte[1024];
        int numRead = 0;
        int pos = 0;
        do {
            int avail = data.length - pos;
            if (avail == 0) {
                int newSize = 2 * data.length;
                byte[] newData = new byte[newSize];
                System.arraycopy(data, 0, newData, 0, data.length);
                data = newData;
                avail = data.length - pos;
            }
            numRead = stream.read(data, pos, avail);
            if (numRead > 0) {
                pos += numRead;
            }
        } while (numRead >= 0);
        ByteBuffer res = ByteBuffer.wrap(data);
        if (data.length != pos) {
            res.limit(pos);
        }
        return res;
    }
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

