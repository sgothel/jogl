package com.jogamp.opengl.test.junit.jogl.demos.gl2;

import java.net.URLConnection;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * Adapted from
 * http://www.java-tips.org/other-api-tips/jogl/how-to-draw-a-texture-mapped-teapot-with-automatically-generated-texture-coordi.html
 */
public class Teapot implements GLEventListener {

    private GLUT glut;

    /* glTexGen stuff: */
    private float sgenparams[] = { 1.0f, 1.0f, 1.0f, 0.0f };
    
    private Texture tex = null;

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        glut = new GLUT();

        gl.glClearColor(0.5f, 0.5f, 0.5f, 0.0f);

        try {
            URLConnection urlConn = IOUtil.getResource("jogl/util/data/av/test-ntsc01-57x32.png", this.getClass().getClassLoader());
            tex = TextureIO.newTexture(gl, TextureIO.newTextureData(gl.getGLProfile(), urlConn.getInputStream(), false, TextureIO.PNG));
        } catch (Exception e) {
            e.printStackTrace();
        }
        tex.bind(gl);

        // uncomment this and comment the above to see a working texture
        // makeStripeImage();
        // gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
        // gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE,
        // GL2.GL_MODULATE);
        // gl.glTexParameterf(GL2.GL_TEXTURE_1D, GL.GL_TEXTURE_WRAP_S,
        // GL.GL_REPEAT);
        // gl.glTexParameterf(GL2.GL_TEXTURE_1D, GL.GL_TEXTURE_MAG_FILTER,
        // GL.GL_LINEAR);
        // gl.glTexParameterf(GL2.GL_TEXTURE_1D, GL.GL_TEXTURE_MIN_FILTER,
        // GL.GL_LINEAR);
        // gl.glTexImage1D(GL2.GL_TEXTURE_1D, 0, 3, stripeImageWidth, 0,
        // GL.GL_RGB, GL.GL_UNSIGNED_BYTE, stripeImageBuf);

        gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
        
        // gl.glTexGeni(GL2.GL_S, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_OBJECT_LINEAR);
        // gl.glTexGenfv(GL2.GL_S, GL2.GL_OBJECT_PLANE, sgenparams, 0);

        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LESS);
        // gl.glEnable(GL2.GL_TEXTURE_GEN_S);
        // gl.glEnable(GL2.GL_TEXTURE_1D);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glEnable(GL2.GL_AUTO_NORMAL);
        gl.glEnable(GL2.GL_NORMALIZE);
        gl.glFrontFace(GL.GL_CW);
        gl.glCullFace(GL.GL_BACK);
        gl.glMaterialf(GL.GL_FRONT, GL2.GL_SHININESS, 64.0f);
    }

    float angleZ = 0.0f;
    float rotDir = 1.0f;
    public float rotIncr = 0.4f;
    
    @Override
    public void display(GLAutoDrawable gLDrawable) {
        final GL2 gl = gLDrawable.getGL().getGL2();

        tex.bind(gl);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glPushMatrix();
        gl.glRotatef(angleZ, 0.0f, 1.0f, 0.0f);
        gl.glRotatef(45.0f, 0.0f, 0.0f, 1.0f);
        glut.glutSolidTeapot(2.0f);
        gl.glPopMatrix();
        gl.glFlush();
        if( angleZ >= 180.0f ) {
            rotDir = -1.0f;
        } else if (angleZ <= 0.0f ) {
            rotDir = +1.0f;
        }
        angleZ += rotIncr * rotDir;    
    }

    @Override
    public void reshape(GLAutoDrawable gLDrawable, int x, int y, int w, int h) {
        GL2 gl = gLDrawable.getGL().getGL2();

        gl.glViewport(0, 0, w, h);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        if (w <= h) {
            gl.glOrtho(-3.5, 3.5, -3.5 * (float) h / (float) w, 
                        3.5 * (float) h / (float) w, -3.5, 3.5);
        } else {
            gl.glOrtho(-3.5 * (float) w / (float) h,
                        3.5 * (float) w / (float) h, -3.5, 3.5, -3.5, 3.5);
        }
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void dispose(GLAutoDrawable gLDrawable) {
    }
}
