package com.jogamp.opengl.util.glsl.sdk;

import com.jogamp.common.util.IOUtil;

import com.jogamp.opengl.*;
import com.jogamp.opengl.util.glsl.*;

import java.io.*;
import java.net.*;

/**
 * Precompiles a shader into a vendor binary format. Input is the
 * resource name of the shader, such as
 * "com/jogamp/opengl/impl/glsl/fixed/shader/a.fp".
 * Output is "com/jogamp/opengl/impl/glsl/fixed/shader/bin/nvidia/a.bfp".
 *
 * All path and suffixes are determined by the ShaderCode class,
 * which ensures runtime compatibility.
 *
 * @see com.jogamp.opengl.util.glsl.ShaderCode
 */

public abstract class CompileShader {

    public abstract int getBinaryFormat();

    public abstract File getSDKCompilerDir();

    public abstract String getVertexShaderCompiler();

    public abstract String getFragmentShaderCompiler();

    public void processOneShader(final String resourceName)
        throws IOException, UnsupportedEncodingException, InterruptedException
    {
        int type = -1;
        String outName=null;
        int suffixLen = -1;
        if(resourceName.endsWith(ShaderCode.getFileSuffix(false, GL2ES2.GL_FRAGMENT_SHADER))) {
            suffixLen = 2;
            type = GL2ES2.GL_FRAGMENT_SHADER;
        } else if(resourceName.endsWith(".frag")) {
            suffixLen = 4;
            type = GL2ES2.GL_FRAGMENT_SHADER;
        } else if(resourceName.endsWith(ShaderCode.getFileSuffix(false, GL2ES2.GL_VERTEX_SHADER))) {
            suffixLen = 2;
            type = GL2ES2.GL_VERTEX_SHADER;
        } else if(resourceName.endsWith(".vert")) {
            suffixLen = 4;
            type = GL2ES2.GL_VERTEX_SHADER;
        }
        final String justName = basename(resourceName);
        outName = justName.substring(0, justName.length() - suffixLen) +
                  ShaderCode.getFileSuffix(true, type);
        final URL resourceURL = IOUtil.getResource(resourceName, this.getClass().getClassLoader(), null).getURL();
        final String dirName = dirname(resourceURL.getPath());

        outName = dirName + File.separator + "bin" + File.separator +
                  ShaderCode.getBinarySubPath(getBinaryFormat()) + File.separator +
                  outName;
        processOneShader(resourceName, outName, type);
    }

    public void processOneShader(final String resourceName, final String outName, final int type)
        throws IOException, UnsupportedEncodingException, InterruptedException
    {
        final URL resourceURL = IOUtil.getResource(resourceName, this.getClass().getClassLoader(), null).getURL();
        final String dirName = dirname(resourceURL.getPath());

        final CharSequence shader = ShaderCode.readShaderSource(null, resourceName, false);
        if(null==shader) {
            System.err.println("Can't find shader source " + resourceName + " - ignored");
            return;
        }
        System.err.println("Preprocessing: "+ resourceName+", in dir: "+dirName);
        final String justName = basename(resourceName);
        String processor;
        switch (type) {
            case GL2ES2.GL_VERTEX_SHADER:
                processor = getVertexShaderCompiler();
                break;
            case GL2ES2.GL_FRAGMENT_SHADER:
                processor = getFragmentShaderCompiler();
                break;
            default:
                throw new GLException("Unknown shader type: "+type);
        }
        final File outputFile = new File(outName);

        // Write shader to a file in java.io.tmpdir
        final File tmpDir = new File(dirName+File.separator+"tmp");
        tmpDir.mkdirs();
        final File tmpFile = new File(tmpDir, justName);
        final Writer writer = new BufferedWriter(new FileWriter(tmpFile));
        writer.write(shader.toString(), 0, shader.length());
        writer.flush();
        writer.close();
        System.err.println("Preprocessed: "+ tmpFile.getAbsolutePath());

        final File processorDir = getSDKCompilerDir();

        System.err.println("SDK: "+ processorDir.getAbsolutePath() + ", compiler: "+processor);

        System.err.println("Output: "+ outputFile.getAbsolutePath());

        // Run the tool
        final Process process = Runtime.getRuntime().exec(new String[] {
                processorDir.getAbsolutePath() + File.separator + processor,
                tmpFile.getAbsolutePath(),
                outputFile.getAbsolutePath()
            }); // , null, processorDir);
        new IOUtil.StreamMonitor( new InputStream[] { process.getInputStream(), process.getErrorStream() }, System.out, null );
        process.waitFor();
        // Delete the temporary file
        // tmpFile.delete();
    }

    protected static String basename(final String path) {
        int lastSlash = path.lastIndexOf("/");
        if (lastSlash < 0) {
            lastSlash = path.lastIndexOf("\\");
        }
        String basename;
        if (lastSlash < 0) {
            basename = path;
        } else {
            basename = path.substring(lastSlash + 1);
        }
        return basename;
    }

    protected static String dirname(final String path) {
        int lastSlash = path.lastIndexOf("/");
        if (lastSlash < 0) {
            lastSlash = path.lastIndexOf("\\");
        }
        String dirname;
        if (lastSlash < 0) {
            dirname = "";
        } else {
            dirname = path.substring(0, lastSlash + 1);
        }
        return dirname;
    }

    public void run(final String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                processOneShader(args[i]);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
