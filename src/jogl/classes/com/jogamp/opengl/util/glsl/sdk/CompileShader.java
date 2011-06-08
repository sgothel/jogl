package com.jogamp.opengl.util.glsl.sdk;

import com.jogamp.common.util.IOUtil;

import javax.media.opengl.*;
import com.jogamp.opengl.util.*;
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

    public void processOneShader(String resourceName)
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
        String justName = basename(resourceName);
        outName = justName.substring(0, justName.length() - suffixLen) +
                  ShaderCode.getFileSuffix(true, type);
        URL resourceURL = IOUtil.getResource(null, resourceName);
        String dirName = dirname(resourceURL.getPath());

        outName = dirName + File.separator + "bin" + File.separator + 
                  ShaderCode.getBinarySubPath(getBinaryFormat()) + File.separator + 
                  outName;
        processOneShader(resourceName, outName, type);
    }

    public void processOneShader(String resourceName, String outName, int type)
        throws IOException, UnsupportedEncodingException, InterruptedException
    {
        URL resourceURL = IOUtil.getResource(null, resourceName);
        String dirName = dirname(resourceURL.getPath());

        String shader = ShaderCode.readShaderSource(null, resourceName);
        if(null==shader) {
            System.err.println("Can't find shader source " + resourceName + " - ignored");
            return;
        }
        System.err.println("Preprocessing: "+ resourceName+", in dir: "+dirName);
        String justName = basename(resourceName);
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
        File outputFile = new File(outName);

        // Write shader to a file in java.io.tmpdir
        File tmpDir = new File(dirName+File.separator+"tmp");
        tmpDir.mkdirs();
        File tmpFile = new File(tmpDir, justName);
        Writer writer = new BufferedWriter(new FileWriter(tmpFile));
        writer.write(shader, 0, shader.length());
        writer.flush();
        writer.close();
        System.err.println("Preprocessed: "+ tmpFile.getAbsolutePath());

        File processorDir = getSDKCompilerDir();

        System.err.println("SDK: "+ processorDir.getAbsolutePath() + ", compiler: "+processor);

        System.err.println("Output: "+ outputFile.getAbsolutePath());

        // Run the tool
        Process process = Runtime.getRuntime().exec(new String[] {
                processorDir.getAbsolutePath() + File.separator + processor,
                tmpFile.getAbsolutePath(),
                outputFile.getAbsolutePath()
            }); // , null, processorDir);
        new StreamMonitor(process.getInputStream());
        new StreamMonitor(process.getErrorStream());
        process.waitFor();
        // Delete the temporary file
        // tmpFile.delete();
    }

    protected static String basename(String path) {
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

    protected static String dirname(String path) {
        int lastSlash = path.lastIndexOf("/");
        if (lastSlash < 0) {
            lastSlash = path.lastIndexOf("\\");
        }
        String dirname;
        if (lastSlash < 0) {
            dirname = new String();
        } else {
            dirname = path.substring(0, lastSlash + 1);
        }
        return dirname;
    }

    public void run(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                processOneShader(args[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class StreamMonitor implements Runnable {
        private InputStream istream;
        public StreamMonitor(InputStream stream) {
            istream = stream;
            new Thread(this, "Output Reader Thread").start();
        }

        public void run()
        {
            byte[] buffer = new byte[4096];
            try {
                int numRead = 0;
                do {
                    numRead = istream.read(buffer);
                    if (numRead > 0) {
                        System.out.write(buffer, 0, numRead);
                        System.out.flush();
                    }
                } while (numRead >= 0);
            }
            catch (IOException e) {
                try {
                    istream.close();
                } catch (IOException e2) {
                }
                // Should allow clean exit when process shuts down
            }
        }
    }
}
