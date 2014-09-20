/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.jogl.demos.gl2.awt;

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.fixedfunc.GLLightingFunc;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.fixedfunc.GLPointerFunc;
import javax.swing.JApplet;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JFrame;

import com.jogamp.common.util.VersionUtil;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * Bug 818: OSX GLJPanel [and GLCanvas] Crash
 * <pre>
 *   - NVIDIA GeForce GT 330M
 *     - GL_VENDOR: "NVIDIA Corporation"
 *     - GL_RENDERER: "NVIDIA GeForce GT 330M OpenGL Engine"
 *     - GL_VERSION: "2.1 NVIDIA-8.12.47 310.40.00.05f01"
 *   - Mac OSX 10.6.8
 * </pre>
 */
public class Bug818GLJPanelAndGLCanvasApplet extends JApplet {

    private static final long serialVersionUID = 1L;

    private Animator animatorCanvas;

    private Animator animatorPanel;

  public static JFrame frame;
  public static JPanel appletHolder;
  public static boolean isApplet = true;

  static public void main(final String args[]) {
    isApplet = false;

    final JApplet myApplet = new Bug818GLJPanelAndGLCanvasApplet();

    appletHolder = new JPanel();

    frame = new JFrame("Bug818GLJPanelApplet");
    frame.getContentPane().add(myApplet);

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(final WindowEvent e) {
        System.exit(0);
      }
    });

    try {
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                myApplet.init();
                frame.validate();
                frame.pack();
                frame.setVisible(true);
            } } );
    } catch( final Throwable throwable ) {
        throwable.printStackTrace();
    }

    myApplet.start();
  }


    @Override
    public void init() {

        final JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2));
        System.err.println("Pre  Orientation L2R: "+panel.getComponentOrientation().isLeftToRight());
        panel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        System.err.println("Post Orientation L2R: "+panel.getComponentOrientation().isLeftToRight());
        setContentPane(panel);
        panel.add(new JLabel("GLJPanel"));
        panel.add(new JLabel("GLCanvas"));

        final GLJPanel gljPanel = new GLJPanel();
        gljPanel.addGLEventListener(new JOGLQuad(false));
        animatorPanel = new Animator(gljPanel);
        gljPanel.setPreferredSize(new Dimension(300, 300));
        panel.add(gljPanel);

        final GLCanvas glCanvas = new GLCanvas();
        glCanvas.addGLEventListener(new JOGLQuad(true));
        animatorCanvas = new Animator(glCanvas);
        glCanvas.setPreferredSize(new Dimension(300, 300));
        panel.add(glCanvas);
    }

    @Override
    public void start() {

        animatorCanvas.start();
        animatorCanvas.setUpdateFPSFrames(60, System.err);
        animatorPanel.start();
        animatorPanel.setUpdateFPSFrames(60, System.err);
    }

    @Override
    public void stop() {

        animatorCanvas.stop();
        animatorPanel.stop();
    }

    @Override
    public void destroy() {}

    /**
     * Self-contained example (within a single class only to keep it simple) displaying a rotating quad
     */
    static class JOGLQuad implements GLEventListener {

        private static final float[] VERTEX_DATA = {
            -1.0f, 1.0f, 0.0f,      // Top Left
            1.0f, 1.0f, 0.0f,       // Top Right
            1.0f, -1.0f, 0.0f,      // Bottom Right
            -1.0f, -1.0f, 0.0f      // Bottom Left
        };

        private static final float[] TEXCOORD_DATA = {
            0.0f, 1.0f,     // Top Left
            1.0f, 1.0f,     // Top Right
            1.0f, 0.0f,     // Bottom Right
            0.0f, 0.0f      // Bottom Left
        };

        private final FloatBuffer vertexBuf;

        private final FloatBuffer texCoordBuf;

        private int vertexVBO;

        private int texCoordVBO;

        private float rotateT = 0.0f;

        private final boolean canvas;

        private Texture texture;

        JOGLQuad(final boolean canvas) {

            this.canvas = canvas;

            ByteBuffer bb = ByteBuffer.allocateDirect(VERTEX_DATA.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuf = bb.asFloatBuffer();
            vertexBuf.put(VERTEX_DATA);
            vertexBuf.rewind();

            bb = ByteBuffer.allocateDirect(TEXCOORD_DATA.length * 4);
            bb.order(ByteOrder.nativeOrder());
            texCoordBuf = bb.asFloatBuffer();
            texCoordBuf.put(TEXCOORD_DATA);
            texCoordBuf.rewind();
        }

        @Override
        public void init(final GLAutoDrawable glDrawable) {

            final GL2 gl = glDrawable.getGL().getGL2();

            System.err.println(VersionUtil.getPlatformInfo());
            System.err.println(JoglVersion.getGLInfo(gl, null, false /* withCapabilitiesAndExtensionInfo */).toString());

            gl.glShadeModel(GLLightingFunc.GL_SMOOTH);
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClearDepth(1.0f);
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthFunc(GL.GL_LEQUAL);
            gl.glHint(GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);

            final int[] tmp = new int[2];
            gl.glGenBuffers(tmp.length, tmp, 0);
            vertexVBO = tmp[0];
            texCoordVBO = tmp[1];

            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexVBO);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, VERTEX_DATA.length * 4, vertexBuf, GL.GL_STATIC_DRAW);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, texCoordVBO);
            gl.glBufferData(GL.GL_ARRAY_BUFFER, TEXCOORD_DATA.length * 4, texCoordBuf, GL.GL_STATIC_DRAW);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

            try {
                final InputStream stream = getClass().getClassLoader().getResourceAsStream("com/jogamp/opengl/test/junit/jogl/util/texture/test-ntscN_3-01-160x90-90pct-yuv444-base.jpg");
                texture = TextureIO.newTexture(stream, true, TextureIO.JPG);
            } catch (final Exception exc) {
                exc.printStackTrace(System.err);
            }
        }

        @Override
        public void dispose(final GLAutoDrawable drawable) {

            final GL2 gl = drawable.getGL().getGL2();
            final int[] tmp = new int[] {vertexVBO, texCoordVBO};
            gl.glGenBuffers(tmp.length, tmp, 0);
        }

        @Override
        public void reshape(final GLAutoDrawable gLDrawable, final int x, final int y, final int width, final int height) {

            final GL2 gl = gLDrawable.getGL().getGL2();
            final float aspect = (float) width / (float) height;
            gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            gl.glLoadIdentity();
            final float fh = 0.5f;
            final float fw = fh * aspect;
            gl.glFrustumf(-fw, fw, -fh, fh, 1.0f, 1000.0f);
            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            gl.glLoadIdentity();
        }

        @Override
        public void display(final GLAutoDrawable gLDrawable) {

            final GL2 gl = gLDrawable.getGL().getGL2();

            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            gl.glLoadIdentity();
            gl.glTranslatef(0.0f, 0.0f, -5.0f);

            // rotate about the three axes
            gl.glRotatef(rotateT, 1.0f, 0.0f, 0.0f);
            gl.glRotatef(rotateT, 0.0f, 1.0f, 0.0f);
            gl.glRotatef(rotateT, 0.0f, 0.0f, 1.0f);

            // set the color of the quad
            if (canvas) {
                gl.glColor3f(0.2f, 0.2f, 1.0f);
            } else {
                gl.glColor3f(1.0f, 0.2f, 0.2f);
            }

            if (texture != null) {
                texture.bind(gl);
                texture.enable(gl);
            } else {
                System.err.println("no texture");
            }

            // Draw A Quad
            gl.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexVBO);
            gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, texCoordVBO);
            gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, 0);
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            gl.glDrawArrays(GL2GL3.GL_QUADS, 0, 4);
            gl.glDisableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
            gl.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);

            if (texture != null) {
                texture.disable(gl);
            }

            // increasing rotation for the next iteration
            rotateT += 0.2f;
        }

    }
}

