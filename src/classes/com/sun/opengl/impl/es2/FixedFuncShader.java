
package com.sun.opengl.impl.es2;

import javax.media.opengl.util.*;
import javax.media.opengl.*;
import java.nio.*;

public class FixedFuncShader {
    public FixedFuncShader(GL2ES2 gl, PMVMatrix pmvMatrix, ShaderData shaderData) {
        init(gl, pmvMatrix, shaderData);
    }

    public boolean isShaderDataValid() {
        return shaderData.isValid();
    }
    public boolean isValid() {
        return shaderData.isValid() && shaderOk;
    }

    public void glUseProgram(GL2ES2 gl, boolean on) {
        if(shaderInUse==on) return;
        if(!shaderOk) return;
        gl.glUseProgram(on?shaderProgram:0);
        shaderInUse = on;
    }

    public void release(GL2ES2 gl) {
        glUseProgram(gl, false);
        gl.glDetachShader(shaderProgram, shaderData.vertexShader());
        gl.glDetachShader(shaderProgram, shaderData.fragmentShader());
        gl.glDeleteProgram(shaderProgram);
    }

    public void syncUniforms(GL2ES2 gl) {
        if(!shaderOk) return;
        if(pmvMatrix.isDirty()) {
            glUseProgram(gl, true);
            gl.glUniformMatrix4fv(shaderPMVMatrix, 2, false, pmvMatrix.glGetPMVMatrixf());
            pmvMatrix.clear();
        }
    }

    public int glArrayName2AttribName(int glName) {
        switch(glName) {
            case GL.GL_VERTEX_ARRAY:
                return VERTEX_ARRAY;
            case GL.GL_COLOR_ARRAY:
                return COLOR_ARRAY;
            case GL.GL_NORMAL_ARRAY:
                return NORMAL_ARRAY;
            case GL.GL_TEXTURE_COORD_ARRAY:
                return TEXCOORD_ARRAY;
        }
        return -1;
    }

    public void glEnableClientState(GL2ES2 gl, int glArrayName) {
        if(!shaderOk) return;
        int attribName = glArrayName2AttribName(glArrayName);
        if(attribName>=0) {
            glUseProgram(gl, true);
            arrayInUse |= (1<<attribName);
            gl.glEnableVertexAttribArray(attribName);
        }
    }

    public void glDisableClientState(GL2ES2 gl, int glArrayName) {
        if(!shaderOk) return;
        int attribName = glArrayName2AttribName(glArrayName);
        if(attribName>=0) {
            glUseProgram(gl, true);
            gl.glDisableVertexAttribArray(attribName);
            arrayInUse &= ~(1<<attribName);
            if(0==arrayInUse) {
                glUseProgram(gl, false);
            }
        }
    }

    public void glVertexPointer(GL2ES2 gl, int size, int type, int stride, Buffer data ) {
        if(!shaderOk) return;
        glUseProgram(gl, true);
        gl.glVertexAttribPointer(VERTEX_ARRAY, size, type, false, stride, data);
    }

    public void glVertexPointer(GL2ES2 gl, int size, int type, int stride, long offset ) {
        if(!shaderOk) return;
        glUseProgram(gl, true);
        gl.glVertexAttribPointer(VERTEX_ARRAY, size, type, false, stride, offset);
    }

    public void glColorPointer(GL2ES2 gl, int size, int type, int stride, Buffer data ) {
        if(!shaderOk) return;
        glUseProgram(gl, true);
        gl.glVertexAttribPointer(COLOR_ARRAY, size, type, false, stride, data);
    }

    public void glColorPointer(GL2ES2 gl, int size, int type, int stride, long offset ) {
        if(!shaderOk) return;
        glUseProgram(gl, true);
        gl.glVertexAttribPointer(COLOR_ARRAY, size, type, false, stride, offset);
    }

    public void glColor4fv(GL2ES2 gl, FloatBuffer data ) {
        glColorPointer(gl, 4, gl.GL_FLOAT, 0, data);
    }

    public void glNormalPointer(GL2ES2 gl, int type, int stride, Buffer data ) {
        if(!shaderOk) return;
        glUseProgram(gl, true);
        gl.glVertexAttribPointer(NORMAL_ARRAY, 3, type, false, stride, data);
    }

    public void glNormalPointer(GL2ES2 gl, int type, int stride, long offset ) {
        if(!shaderOk) return;
        glUseProgram(gl, true);
        gl.glVertexAttribPointer(NORMAL_ARRAY, 3, type, false, stride, offset);
    }

    public void glTexCoordPointer(GL2ES2 gl, int size, int type, int stride, Buffer data ) {
        if(!shaderOk) return;
        glUseProgram(gl, true);
        gl.glVertexAttribPointer(TEXCOORD_ARRAY, size, type, false, stride, data);
    }

    public void glTexCoordPointer(GL2ES2 gl, int size, int type, int stride, long offset ) {
        if(!shaderOk) return;
        glUseProgram(gl, true);
        gl.glVertexAttribPointer(TEXCOORD_ARRAY, size, type, false, stride, offset);
    }

    public void setVertexAttribPointer(GL2ES2 gl, int glArrayName, int size, int type, int stride, Buffer data ) {
        int attribName = glArrayName2AttribName(glArrayName);
        if(!shaderOk || attribName<0) return;
        glUseProgram(gl, true);
        gl.glEnableVertexAttribArray(attribName);
        gl.glVertexAttribPointer(attribName, size, type, false, stride, data);
    }

    public void setVertexAttribPointer(GL2ES2 gl, int glArrayName, int size, int type, int stride, long offset ) {
        int attribName = glArrayName2AttribName(glArrayName);
        if(!shaderOk || attribName<0) return;
        glUseProgram(gl, true);
        gl.glEnableVertexAttribArray(attribName);
        gl.glVertexAttribPointer(attribName, size, type, false, stride, offset);
    }

    public void glLightfv(GL2ES2 gl, int light, int pname, java.nio.FloatBuffer params) {
        if(!shaderOk) return;
        light -=GL.GL_LIGHT0;
        if(0 <= light && light <= 7) {
            if(gl.GL_POSITION==pname && 0<=shaderLightsSource[light]) {
                glUseProgram(gl, true);
                gl.glUniform4fv(shaderLightsSource[light], 1, params);
            } else if(gl.GL_AMBIENT==pname && 0<=shaderLigthsAmbient[light]) {
                gl.glUniform4fv(shaderLigthsAmbient[light], 1, params);
            }
        }
    }

    public void glShadeModel(GL2ES2 gl, int mode) {
        if(!shaderOk || 0>shaderShadeModel) return;
        glUseProgram(gl, true);
        gl.glUniform1i(shaderShadeModel, mode);
    }

    public void glActiveTexture(GL2ES2 gl, int texture) {
        if(!shaderOk || 0>shaderShadeModel) return;
        glUseProgram(gl, true);
        texture-=gl.GL_TEXTURE0;
        gl.glUniform1i(shaderActiveTexture, 0);
    }

    protected void init(GL2ES2 gl, PMVMatrix pmvMatrix, ShaderData shaderData) {
        if(shaderOk) return;

        if(null==pmvMatrix) {
            throw new GLException("PMVMatrix is null");
        }
        this.pmvMatrix=pmvMatrix;
        this.shaderData=shaderData;

        if(!shaderData.createAndCompile(gl)) {
            return;
        }

        // Create the shader program
        shaderProgram = gl.glCreateProgram();

        // Attach the fragment and vertex shaders to it
        gl.glAttachShader(shaderProgram, shaderData.vertexShader());
        gl.glAttachShader(shaderProgram, shaderData.fragmentShader());

        gl.glBindAttribLocation(shaderProgram, VERTEX_ARRAY, "mgl_Vertex");
        gl.glBindAttribLocation(shaderProgram, COLOR_ARRAY, "mgl_Color");
        gl.glBindAttribLocation(shaderProgram, TEXCOORD_ARRAY, "mgl_MultiTexCoord0");

        // Link the program
        gl.glLinkProgram(shaderProgram);

        if ( ! gl.glIsProgramValid(shaderProgram, System.err) )  {
            return;
        }

        gl.glUseProgram(shaderProgram);

        shaderPMVMatrix = gl.glGetUniformLocation(shaderProgram, "mgl_PMVMatrix");
        if(0<=shaderPMVMatrix) {
            gl.glUniformMatrix4fv(shaderPMVMatrix, 2, false, pmvMatrix.glGetPMVMatrixf());
            pmvMatrix.clear();
            shaderOk = true;
        } else {
            System.err.println("could not get uniform mgl_PMVMatrix: "+shaderPMVMatrix);
        }

        // optional parameter ..
        for(int i=0; i<7; i++) {
            shaderLightsSource[i] = gl.glGetUniformLocation(shaderProgram, "mgl_LightSource"+i);
            shaderLigthsAmbient[i] = gl.glGetUniformLocation(shaderProgram, "mgl_LightAmbient"+i);
        }
        shaderShadeModel = gl.glGetUniformLocation(shaderProgram, "mgl_ShadeModel");

        shaderActiveTexture = gl.glGetUniformLocation(shaderProgram, "mgl_ActiveTexture");
        if(0<=shaderActiveTexture) {
            gl.glUniform1i(shaderActiveTexture, 0);
        }

        gl.glUseProgram(0);
        shaderInUse = false;
    }

    protected PMVMatrix pmvMatrix;
    protected ShaderData shaderData;

    protected boolean shaderOk = false;
    protected boolean shaderInUse = false;
    protected int arrayInUse = 0;
    protected int shaderProgram=-1;

    // attributes
    protected static final int VERTEX_ARRAY   = 0; // mgl_Vertex
    protected static final int COLOR_ARRAY    = 1; // mgl_Color
    protected static final int NORMAL_ARRAY   = 2; // ?
    protected static final int TEXCOORD_ARRAY = 3; // mgl_MultiTexCoord0

    // uniforms ..
    protected int shaderPMVMatrix=-1; // mgl_PMVMatrix mat4
    protected int[] shaderLightsSource = new int[] { -1, -1, -1, -1, -1, -1, -1 }; // vec4f mgl_LightSourcei
    protected int[] shaderLigthsAmbient = new int[] { -1, -1, -1, -1, -1, -1, -1 }; // vec4f mgl_LightAmbienti
    protected int   shaderShadeModel = -1; // mgl_ShadeModel int
    protected int   shaderActiveTexture = -1; // mgl_ActiveTexture int
}

