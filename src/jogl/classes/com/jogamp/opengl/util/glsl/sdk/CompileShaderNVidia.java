package com.jogamp.opengl.util.glsl.sdk;

import com.jogamp.opengl.*;

import java.io.*;

/** Precompiles a shader into NVidia binary format. Input is the
    resource name of the shader, such as
    "com/jogamp/opengl/impl/glsl/fixed/shader/a.fp".
    Output is "com/jogamp/opengl/impl/glsl/fixed/shader/bin/nvidia/a.bfp". */

public class CompileShaderNVidia extends CompileShader {
    private static final String NVAPSDK;

    static {
        final String nvapSDKProp = System.getProperty("NVAPSDK");
        if (nvapSDKProp != null) {
            NVAPSDK = nvapSDKProp;
        } else {
            NVAPSDK = "C:\\nvap_sdk_0_3_x";
        }
    }

    @Override
    public int getBinaryFormat() {
        return GLES2.GL_NVIDIA_PLATFORM_BINARY_NV;
    }

    @Override
    public File getSDKCompilerDir() {
        File compilerDir = new File( NVAPSDK + File.separator + "tools" + File.separator );
        File compilerFile = new File( compilerDir, getVertexShaderCompiler());
        if(!compilerFile.exists()) {
            compilerDir = new File( NVAPSDK );
            compilerFile = new File( compilerDir, getVertexShaderCompiler());
        }
        if(!compilerFile.exists()) {
            throw new GLException("Can't find compiler: "+getVertexShaderCompiler() + " in : " +
                                  NVAPSDK+", "+NVAPSDK + File.separator + "tools");
        }
        return compilerDir;
    }

    @Override
    public String getVertexShaderCompiler() {
        return "glslv.bat";
    }

    @Override
    public String getFragmentShaderCompiler() {
        return "glslf.bat";
    }

    public static void main(final String[] args) {
        new CompileShaderNVidia().run(args);
    }
}
