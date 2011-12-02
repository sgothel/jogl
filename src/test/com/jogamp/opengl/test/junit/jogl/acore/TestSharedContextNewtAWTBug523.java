/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
 
package com.jogamp.opengl.test.junit.jogl.acore;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;


/**
 * TestSharedContextNewtAWTBug523
 * 
 * Opens a single JFrame with two OpenGL windows and sliders to adjust the view orientation.
 *
 * Each window renders a red triangle and a blue triangle.
 * The red triangle is rendered using glBegin / glVertex / glEnd.
 * The blue triangle is rendered using vertex buffer objects bound to a vertex array object.
 *
 * If static useNewt is true, then those windows are GLWindow objects in a NewtCanvasAWT.
 * If static useNewt is false, then those windows are GLCanvas objects.
 *
 * If shareContext is true, then the two OpenGL windows are initialized with a shared context,
 * so that they share the vertex buffer and array objects and display lists.
 * If shareContext is false, then the two OpenGL windows each have their own context, and the blue
 * triangle fails to render in one of the windows.
 * 
 * The four test cases run through the four combinations of useNewt and shareContext.
 * 
 * Similar test cases are {@link TestSharedContextListNEWT}, {@link TestSharedContextListAWT}, 
 * {@link TestSharedContextVBOES2NEWT} and {@link TestSharedContextVBOES1NEWT}.
 * 
 */
public class TestSharedContextNewtAWTBug523 extends UITestCase {

    static long durationPerTest = 1000;
    
    // counters for instances of event listener TwoTriangles
    // private static int instanceCounter;
    private static int initializationCounter;

    // This semaphore is released once each time a GLEventListener destroy method is called.
    // The main thread waits twice on this semaphore to ensure both canvases have finished cleaning up.
    private static Semaphore disposalCompleteSemaphore = new Semaphore(0);

    // Buffer objects can be shared across shared OpenGL context. 
    // If we run with sharedContext, then the tests will use these shared buffer objects,
    // otherwise each event listener allocates its own buffer objects.
    private static int [] sharedVertexBufferObjects = {0};
    private static int [] sharedIndexBufferObjects = {0};
    private static FloatBuffer sharedVertexBuffer;
    private static IntBuffer sharedIndexBuffer;

    static private GLPbuffer initShared(GLCapabilities caps) {
        GLPbuffer sharedDrawable = GLDrawableFactory.getFactory(caps.getGLProfile()).createGLPbuffer(null, caps, null, 64, 64, null);
        Assert.assertNotNull(sharedDrawable);
        // init and render one frame, which will setup the Gears display lists
        sharedDrawable.display();
        return sharedDrawable;
    }

    static private void releaseShared(GLPbuffer sharedDrawable) {
        if(null != sharedDrawable) {
            sharedDrawable.destroy();
        }
    }
    
    // inner class that implements the event listener
    static class TwoTriangles implements GLEventListener {

        boolean useShared;
        int canvasWidth;
        int canvasHeight;
        private float boundsRadius = 2f;
        private float viewDistance;
        private float viewDistanceFactor = 1.0f;
        private float xAxisRotation;
        private float yAxisRotation;
        private float viewFovDegrees = 15f;

        // vertex array objects cannot be shared across open gl canvases;
        //
        // However, display lists can be shared across GLCanvas instances (if those canvases are initialized with the same GLContext),
        // including a display list created in one context that uses a VAO. 
        private int [] vertexArrayObjects = {0};

        // Buffer objects can be shared across canvas instances, if those canvases are initialized with the same GLContext.
        // If we run with sharedBufferObjects true, then the tests will use these shared buffer objects.
        // If we run with sharedBufferObjects false, then each event listener allocates its own buffer objects.
        private int [] privateVertexBufferObjects = {0};
        private int [] privateIndexBufferObjects = {0};
        private FloatBuffer privateVertexBuffer;
        private IntBuffer privateIndexBuffer;
        
        TwoTriangles (int canvasWidth, int canvasHeight, boolean useShared) {
            // instanceNum = instanceCounter++;
            this.canvasWidth = canvasWidth;
            this.canvasHeight = canvasHeight;
            this.useShared = useShared;
        }

        public void setXAxisRotation(float xRot) {
            xAxisRotation = xRot;
        }

        public void setYAxisRotation(float yRot) {
            yAxisRotation = yRot;
        }

        public void setViewDistanceFactor(float factor) {
            viewDistanceFactor = factor;
        }


        public void init(GLAutoDrawable drawable) {
            GL2 gl2 = drawable.getGL().getGL2();

            System.err.println("INIT GL IS: " + gl2.getClass().getName());

            // Disable VSync
            gl2.setSwapInterval(0);

            // Setup the drawing area and shading mode
            gl2.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

            // the first instance of TwoTriangles initializes the shared buffer objects;
            // synchronizing to deal with potential liveness issues if the data is intialized from one thread and used on another
            synchronized (this) {
                gl2.glGenVertexArrays(1, vertexArrayObjects, 0);

                gl2.glBindVertexArray(vertexArrayObjects[0]);

                // use either the shared or private vertex buffers, as 
                int [] vertexBufferObjects;
                int [] indexBufferObjects;
                FloatBuffer vertexBuffer;
                IntBuffer indexBuffer;
                //
                if (useShared) {
                    vertexBufferObjects = sharedVertexBufferObjects;
                    indexBufferObjects = sharedIndexBufferObjects;
                    vertexBuffer = sharedVertexBuffer;
                    indexBuffer = sharedIndexBuffer;
                } else {
                    vertexBufferObjects = privateVertexBufferObjects;
                    indexBufferObjects = privateIndexBufferObjects;
                    vertexBuffer = privateVertexBuffer;
                    indexBuffer = privateIndexBuffer;
                }
                
                // if buffer sharing is enabled, then the first of the two event listeners to be
                // initialized will allocate the buffers, and the other will re-use the allocated one
                if (vertexBufferObjects[0] == 0) {
                    vertexBuffer = GLBuffers.newDirectFloatBuffer(18);
                    vertexBuffer.put(new float[]{
                            1.0f, -0.5f, 0f,    // vertex 1
                            0f, 0f, 1f,         // normal 1
                            1.5f, -0.5f, 0f,    // vertex 2
                            0f, 0f, 1f,         // normal 2
                            1.0f, 0.5f, 0f,     // vertex 3
                            0f, 0f, 1f          // normal 3
                    });
                    vertexBuffer.position(0);

                    gl2.glGenBuffers(1, vertexBufferObjects, 0);
                    gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, vertexBufferObjects[0]);
                    gl2.glBufferData(GL2.GL_ARRAY_BUFFER, vertexBuffer.capacity() * Buffers.SIZEOF_FLOAT, vertexBuffer, GL2.GL_STATIC_DRAW);
                }
                
                // A check in the case that buffer sharing is enabled but context sharing is not enabled -- in that
                // case, the buffer objects are not shareable, and the blue triangle cannot be rendereds.
                // Furthermore, although the calls to bind and draw elements do not cause an error from glGetError
                // when this check is removed, true blue triangle is not rendered anyways, and more importantly,
                // I found that with my system glDrawElements causes a runtime exception 50% of the time. Executing the binds
                // to unshareable buffers sets up glDrawElements for unpredictable crashes -- sometimes it does, sometimes not.
                if (gl2.glIsBuffer(vertexBufferObjects[0])) {
                    gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, vertexBufferObjects[0]);
                    //
                    gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
                    gl2.glVertexPointer(3, GL2.GL_FLOAT, 6 * GLBuffers.SIZEOF_FLOAT, 0);
                    //
                    gl2.glEnableClientState(GL2.GL_NORMAL_ARRAY);
                    gl2.glNormalPointer(GL2.GL_FLOAT, 6 * GLBuffers.SIZEOF_FLOAT, 3 * GLBuffers.SIZEOF_FLOAT);
                }

                if (indexBufferObjects[0] == 0) {
                    indexBuffer = GLBuffers.newDirectIntBuffer(3);
                    indexBuffer.put(new int[]{0, 1, 2});
                    indexBuffer.position(0);

                    gl2.glGenBuffers(1, indexBufferObjects, 0);
                    gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferObjects[0]);
                    gl2.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * Buffers.SIZEOF_INT, indexBuffer, GL2.GL_STATIC_DRAW);
                }

                // again, a check in the case that buffer sharing is enabled but context sharing is not enabled 
                if (gl2.glIsBuffer(indexBufferObjects[0])) {
                    gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferObjects[0]);
                }

                gl2.glBindVertexArray(0);
                gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
                gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
                gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
                gl2.glDisableClientState(GL2.GL_NORMAL_ARRAY);

                initializationCounter++;
            } // synchronized (this)


            viewDistance = setupViewFrustum(gl2, canvasWidth, canvasHeight, boundsRadius, 1.0f, viewFovDegrees);

        }

        public void dispose(GLAutoDrawable drawable) {

            synchronized (this) {
                initializationCounter--;

                GL2 gl2 = drawable.getGL().getGL2();

                gl2.glDeleteVertexArrays(1, vertexArrayObjects, 0);
                vertexArrayObjects[0] = 0;
                logAnyErrorCodes(gl2, "display");

                // release shared resources 
                if (initializationCounter == 0 || !useShared) {
                    // use either the shared or private vertex buffers, as 
                    int [] vertexBufferObjects;
                    int [] indexBufferObjects;
                    if (useShared) {
                        vertexBufferObjects = sharedVertexBufferObjects;
                        indexBufferObjects = sharedIndexBufferObjects;
                    } else {
                        vertexBufferObjects = privateVertexBufferObjects;
                        indexBufferObjects = privateIndexBufferObjects;
                    }

                    gl2.glDeleteBuffers(1, vertexBufferObjects, 0);
                    gl2.glDeleteBuffers(1, indexBufferObjects, 0);
                    vertexBufferObjects[0] = 0;
                    indexBufferObjects[0] = 0;
                    logAnyErrorCodes(gl2, "display");
                }

                // release the main thread once for each disposal
                disposalCompleteSemaphore.release();
            } // synchronized (this)
        }

        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        }

        public void display(GLAutoDrawable drawable) {

            // wait until all instances are initialized before attempting to draw using the
            // vertex array object, because the buffers are allocated in init and when the
            // buffers are shared, we need to ensure that they are allocated before trying to use thems
            synchronized (this) {
                if (initializationCounter != 2) {
                    return;
                }
            }

            GL2 gl2 = drawable.getGL().getGL2();
            GLU glu = new GLU();

            // Clear the drawing area
            gl2.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

            gl2.glViewport(0, 0, canvasWidth, canvasHeight);
            gl2.glMatrixMode(GL2.GL_PROJECTION);
            gl2.glLoadIdentity();
            glu.gluPerspective(viewFovDegrees, (float)canvasWidth/(float)canvasHeight,
                               viewDistance*viewDistanceFactor-boundsRadius, viewDistance*viewDistanceFactor+boundsRadius);

            // Reset the current matrix to the "identity"
            gl2.glMatrixMode(GL2.GL_MODELVIEW);
            gl2.glLoadIdentity();

            // draw the scene
            gl2.glPushAttrib(GL2.GL_ALL_ATTRIB_BITS);
            gl2.glPushMatrix();

            glu.gluLookAt(0, 0, 0 + viewDistance*viewDistanceFactor, 0, 0, 0, 0, 1, 0);
            gl2.glRotatef(xAxisRotation, 1, 0, 0);
            gl2.glRotatef(yAxisRotation, 0, 1, 0);

            gl2.glDisable(GL2.GL_CULL_FACE);
            gl2.glEnable(GL2.GL_DEPTH_TEST);

            // draw the triangles
            drawTwoTriangles(gl2);

            gl2.glPopMatrix();
            gl2.glPopAttrib();

            // Flush all drawing operations to the graphics card
            gl2.glFlush();

            logAnyErrorCodes(gl2, "display");
        }

        public void drawTwoTriangles(GL2 gl2) {

            // draw a red triangle the old fashioned way
            gl2.glColor3f(1f, 0f, 0f);
            gl2.glBegin(GL2.GL_TRIANGLES);
            gl2.glVertex3d(-1.5, -0.5, 0);
            gl2.glNormal3d(0, 0, 1);
            gl2.glVertex3d(-0.5, -0.5, 0);
            gl2.glNormal3d(0, 0, 1);
            gl2.glVertex3d(-0.75, 0.5, 0);
            gl2.glNormal3d(0, 0, 1);
            gl2.glEnd();

            // draw the blue triangle using a vertex array object, sharing the vertex and index buffer objects across
            // contexts; if context sharing is not initialized, then one window will simply have to live without a blue triangle
            //
            // synchronizing to deal with potential liveness issues if the data is intialized from one
            // thread and used on another
            boolean vaoBound = false;
            // use either the shared or private vertex buffers, as 
            int [] vertexBufferObjects;
            int [] indexBufferObjects;
            synchronized (this) {
                if (useShared) {
                    vertexBufferObjects = sharedVertexBufferObjects;
                    indexBufferObjects = sharedIndexBufferObjects;
                } else {
                    vertexBufferObjects = privateVertexBufferObjects;
                    indexBufferObjects = privateIndexBufferObjects;
                }
            } // synchronized (this)
                
            // A check in the case that buffer sharing is enabled but context sharing is not enabled -- in that
            // case, the buffer objects are not shareable, and the blue triangle cannot be rendereds.
            // Furthermore, although the calls to bind and draw elements do not cause an error from glGetError
            // when this check is removed, true blue triangle is not rendered anyways, and more importantly,
            // I found that with my system glDrawElements causes a runtime exception 50% of the time. Executing the binds
            // to unshareable buffers sets up glDrawElements for unpredictable crashes -- sometimes it does, sometimes not.
            if (gl2.glIsVertexArray(vertexArrayObjects[0]) && 
                    gl2.glIsBuffer(indexBufferObjects[0]) && gl2.glIsBuffer(vertexBufferObjects[0])) {
                gl2.glBindVertexArray(vertexArrayObjects[0]);
                gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferObjects[0]);
                vaoBound = true;
            }
        
            logAnyErrorCodes(gl2, "drawTwoTriangles");

            if (vaoBound) {
                gl2.glColor3f(0f, 0f, 1f);
                gl2.glDrawElements(GL2.GL_TRIANGLES, 3, GL2.GL_UNSIGNED_INT, 0);
                gl2.glBindVertexArray(0);
                gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
            } 
        }

        public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
        }
        
    } // inner class TwoTriangles

    public static void logAnyErrorCodes(GL2 gl2, String prefix) {
        int glError = gl2.glGetError();
        while (glError != GL2.GL_NO_ERROR) {
            System.err.println(prefix + ", OpenGL error: 0x" + Integer.toHexString(glError));
            glError = gl2.glGetError();
        }
        int status = gl2.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
        if (status != GL2.GL_FRAMEBUFFER_COMPLETE) {
            System.err.println(prefix + ", glCheckFramebufferStatus: 0x" + Integer.toHexString(status));
        }
    }

    /**
     * Sets the OpenGL projection matrix and front and back clipping planes for
     * a viewport and returns the distance the camera should be placed from
     * the center of the scene's bounding sphere such that the geometry is
     * centered in the view frustum.
     *
     * @param gl2 current OpenGL context
     * @param width width of GLDrawable
     * @param height height of GLDrawable
     * @param boundsRadius radius of a minimal bounding sphere of objects to be
     *            rendered in the viewport
     * @param zoomFactor affects how far away the camera is placed from the scene; changing the
     *            zoom from 1.0 to 0.5 would make the scene appear half the size
     * @param viewFovDegrees the desired field of vision for the viewport,
     *            higher is more fish-eye
     * @return the distance the camera should be from the center of the scenes
     *         bounding sphere
     */
    public static float setupViewFrustum(GL2 gl2, int width, int height, float boundsRadius, float zoomFactor, float viewFovDegrees) {
    	assert boundsRadius > 0.0f;
    	assert zoomFactor > 0.0f;
    	assert viewFovDegrees > 0.0f;

        GLU glu = new GLU();

        final float aspectRatio = (float) width / (float) height;
        final float boundRadiusAdjusted = boundsRadius / zoomFactor;
        final float lowestFov = aspectRatio > 1.0f ? viewFovDegrees : aspectRatio * viewFovDegrees;
        final float viewDist = (float) (boundRadiusAdjusted / Math.sin( (lowestFov / 2.0) * (Math.PI / 180.0) ));

        gl2.glMatrixMode(GL2.GL_PROJECTION);
        gl2.glLoadIdentity();
        glu.gluPerspective(viewFovDegrees, aspectRatio,	0.1*viewDist, viewDist + boundRadiusAdjusted);

        return viewDist;
    }

    @Test
    public void testContextSharingCreateVisibleDestroy1() throws InterruptedException, InvocationTargetException {
        testContextSharingCreateVisibleDestroy(false, false);
    }

    @Test
    public void testContextSharingCreateVisibleDestroy2() throws InterruptedException, InvocationTargetException {
        testContextSharingCreateVisibleDestroy(false, true);
    }

    @Test
    public void testContextSharingCreateVisibleDestroy3() throws InterruptedException, InvocationTargetException {
        testContextSharingCreateVisibleDestroy(true, false);
    }

    @Test
    public void testContextSharingCreateVisibleDestroy4() throws InterruptedException, InvocationTargetException {
        testContextSharingCreateVisibleDestroy(true, true);
    }

    /**
     * Assemble the user interface and start the animator.
     * It waits until the window is closed an then attempts orderly shutdown and resource deallocation.
     */
    public void testContextSharingCreateVisibleDestroy(final boolean useNewt, final boolean shareContext) throws InterruptedException, InvocationTargetException {
        final JFrame frame = new JFrame("Simple JOGL App for testing context sharing");
        //
        // GLDrawableFactory factory = GLDrawableFactory.getFactory(GLProfile.get(GLProfile.GL2));
        // GLContext sharedContext = factory.getOrCreateSharedContext(factory.getDefaultDevice());
        //
        GLCapabilities glCapabilities = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        glCapabilities.setSampleBuffers(true);
        glCapabilities.setNumSamples(4);

        final GLPbuffer sharedDrawable;
        final GLContext sharedContext; 
        if(shareContext) {
            sharedDrawable = initShared(glCapabilities);
            sharedContext = sharedDrawable.getContext();
        } else {
            sharedDrawable = null;
            sharedContext = null;
        }
        
        final TwoTriangles eventListener1 = new TwoTriangles(640, 480, shareContext);
        final TwoTriangles eventListener2 = new TwoTriangles(320, 480, shareContext);

        final Component openGLComponent1;
        final Component openGLComponent2;
        final GLAutoDrawable openGLAutoDrawable1;
        final GLAutoDrawable openGLAutoDrawable2;

        if (useNewt) {
            GLWindow glWindow1 = GLWindow.create(glCapabilities);
            glWindow1.setSharedContext(sharedContext);
            NewtCanvasAWT newtCanvasAWT1 = new NewtCanvasAWT(glWindow1);
            newtCanvasAWT1.setPreferredSize(new Dimension(eventListener1.canvasWidth, eventListener1.canvasHeight));
            glWindow1.addGLEventListener(eventListener1);
            //
            GLWindow glWindow2 = GLWindow.create(glCapabilities);
            NewtCanvasAWT newtCanvasAWT2 = new NewtCanvasAWT(glWindow2);
            newtCanvasAWT2.setPreferredSize(new Dimension(eventListener2.canvasWidth, eventListener2.canvasHeight));
            glWindow2.addGLEventListener(eventListener2);

            openGLComponent1 = newtCanvasAWT1;
            openGLComponent2 = newtCanvasAWT2;
            openGLAutoDrawable1 = glWindow1;
            openGLAutoDrawable2 = glWindow2;
        } else {
            // Implementation using two GLCanvas instances; for GLCanvas context sharing to work, you must pass it in
            // through the constructor; if you set it after it has no effect -- it does no harm if you initialized the ctor
            // with the shared context, but if you didn't, it also doesn't trigger sharing
            final GLCanvas canvas1;
            final GLCanvas canvas2;

            if (shareContext) {
                canvas1 = new GLCanvas(glCapabilities, sharedContext);
                canvas2 = new GLCanvas(glCapabilities, sharedContext);
            } else {
                canvas1 = new GLCanvas(glCapabilities);
                canvas2 = new GLCanvas(glCapabilities);
            }

            canvas1.setSize(eventListener1.canvasWidth, eventListener1.canvasHeight);
            canvas1.addGLEventListener(eventListener1);
            //
            canvas2.setSize(eventListener2.canvasWidth, eventListener2.canvasHeight);
            canvas2.addGLEventListener(eventListener2);

            openGLComponent1 = canvas1;
            openGLComponent2 = canvas2;
            openGLAutoDrawable1 = canvas1;
            openGLAutoDrawable2 = canvas2;
        }

        // Create slider for x rotation.
        // The vertically oriented slider rotates around the x axis
        final JSlider xAxisRotationSlider = new JSlider(JSlider.VERTICAL, -180, 180, 1);
        xAxisRotationSlider.setPaintTicks(false);
        xAxisRotationSlider.setPaintLabels(false);
        xAxisRotationSlider.setSnapToTicks(false);
        xAxisRotationSlider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                eventListener1.setXAxisRotation(xAxisRotationSlider.getValue());
                eventListener2.setXAxisRotation(xAxisRotationSlider.getValue());
            }
        });
        JLabel xAxisRotationLabel = new JLabel("X-Axis Rotation");

        // Create slider for y rotation.
        // The horizontally oriented slider rotates around the y axis
        final JSlider yAxisRotationSlider = new JSlider(JSlider.HORIZONTAL, -180, 180, 1);
        yAxisRotationSlider.setPaintTicks(false);
        yAxisRotationSlider.setPaintLabels(false);
        yAxisRotationSlider.setSnapToTicks(false);
        yAxisRotationSlider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                eventListener1.setYAxisRotation(yAxisRotationSlider.getValue());
                eventListener2.setYAxisRotation(yAxisRotationSlider.getValue());
            }
        });
        JLabel yAxisRotationLabel = new JLabel("Y-Axis Rotation");

        // Create slider for view distance factor.
        // We want a range of 0.0 to 10.0 with 0.1 increments (so we can scale down using 0.0 to 1.0).
        // So, set JSlider to 0 to 100 and divide by 10.0 in stateChanged
        final JSlider viewDistanceFactorSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);
        viewDistanceFactorSlider.setPaintTicks(false);
        viewDistanceFactorSlider.setPaintLabels(false);
        viewDistanceFactorSlider.setSnapToTicks(false);
        viewDistanceFactorSlider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                eventListener1.setViewDistanceFactor((float) viewDistanceFactorSlider.getValue() / 10.0f);
                eventListener2.setViewDistanceFactor((float) viewDistanceFactorSlider.getValue() / 10.0f);
            }
        });
        JLabel viewDistanceFactorLabel = new JLabel("View Distance Factor");

        // group the view distance and label into a vertical panel
        JPanel viewDistancePanel = new JPanel();
        viewDistancePanel.setLayout(new BoxLayout(viewDistancePanel, BoxLayout.PAGE_AXIS));
        viewDistancePanel.add(Box.createVerticalGlue());
        viewDistancePanel.add(viewDistanceFactorSlider);
        viewDistancePanel.add(viewDistanceFactorLabel);
        viewDistancePanel.add(Box.createVerticalGlue());

        // group both OpenGL canvases / windows into a horizontal panel
        JPanel openGLPanel = new JPanel();
        openGLPanel.setLayout(new BoxLayout(openGLPanel, BoxLayout.LINE_AXIS));
        openGLPanel.add(openGLComponent1);
        openGLPanel.add(Box.createHorizontalStrut(5));
        openGLPanel.add(openGLComponent2);

        // group the open GL panel and the y-axis rotation slider into a vertical panel.
        JPanel canvasAndYAxisPanel = new JPanel();
        canvasAndYAxisPanel.setLayout(new BoxLayout(canvasAndYAxisPanel, BoxLayout.PAGE_AXIS));
        canvasAndYAxisPanel.add(openGLPanel);
        canvasAndYAxisPanel.add(Box.createVerticalGlue());
        canvasAndYAxisPanel.add(yAxisRotationSlider);
        canvasAndYAxisPanel.add(yAxisRotationLabel);

        // group the X-axis rotation slider and label into a horizontal panel.
        JPanel xAxisPanel = new JPanel();
        xAxisPanel.setLayout(new BoxLayout(xAxisPanel, BoxLayout.LINE_AXIS));
        xAxisPanel.add(xAxisRotationSlider);
        xAxisPanel.add(xAxisRotationLabel);

        JPanel mainPanel = (JPanel) frame.getContentPane();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));
        mainPanel.add(viewDistancePanel);
        mainPanel.add(Box.createHorizontalGlue());
        mainPanel.add(canvasAndYAxisPanel);
        mainPanel.add(Box.createHorizontalGlue());
        mainPanel.add(xAxisPanel);

        final Animator animator = new Animator(Thread.currentThread().getThreadGroup());
        animator.setUpdateFPSFrames(1, null);
        animator.add(openGLAutoDrawable1);
        animator.add(openGLAutoDrawable2);

        final Semaphore windowOpenSemaphore = new Semaphore(0);
        final Semaphore closingSemaphore = new Semaphore(0);

        // signal the main thread when the frame is closed
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                closingSemaphore.release();
            }
        });

        // make the window visible using the EDT
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                frame.pack();
                frame.setVisible(true);
                windowOpenSemaphore.release();
            }
        });

        // wait for the window to be visible and start the animation
        try {
            boolean windowOpened = windowOpenSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS);
            Assert.assertEquals(true, windowOpened);
        } catch (InterruptedException e) {
            System.err.println("Closing wait interrupted: " + e.getMessage());
        }
        animator.start();

        // sleep for test duration, then request the window to close, wait for the window to close,s and stop the animation
        try {
            while(animator.isAnimating() && animator.getTotalFPSDuration() < durationPerTest) {
                Thread.sleep(100);
            }
            AWTRobotUtil.closeWindow(frame, true);
            boolean windowClosed = closingSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS);
            Assert.assertEquals(true, windowClosed);
        } catch (InterruptedException e) {
            System.err.println("Closing wait interrupted: " + e.getMessage());
        }
        animator.stop();

        // ask the EDT to dispose of the frame;
        // if using newt, explicitly dispose of the canvases because otherwise it seems our destroy methods are not called
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                frame.setVisible(false);
                frame.dispose();
                if (useNewt) {
                    ((NewtCanvasAWT)openGLComponent1).destroy();
                    ((NewtCanvasAWT)openGLComponent2).destroy();
                }
                closingSemaphore.release();
            }
        });

        // wait for orderly destruction; it seems that if we share a GLContext across newt windows, bad things happen;
        // I must be doing something wrong but I haven't figured it out yet
        try {
            boolean windowsDisposed = closingSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS);
            Assert.assertEquals(true, windowsDisposed);
        } catch (InterruptedException e) {
            System.err.println("Closing wait interrupted: " + e.getMessage());
        }

        // ensure that the two OpenGL canvas' destroy methods completed successfully and released resources before we move on
        int disposalSuccesses = 0;
        try {
            boolean acquired;
            acquired = disposalCompleteSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS);
            if (acquired){
                disposalSuccesses++;
            }
            acquired = disposalCompleteSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS);
            if (acquired){
                disposalSuccesses++;
            }
        } catch (InterruptedException e) {
            System.err.println("Clean exit interrupted: " + e.getMessage());
        }

        Assert.assertEquals(true, disposalSuccesses == 2);
        
        releaseShared(sharedDrawable);
    }

    static int atoi(String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(String[] args) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                if (++i < args.length) {
                    durationPerTest = atoi(args[i]);
                }
            } 
        }
        
        String testname = TestSharedContextNewtAWTBug523.class.getName();
        org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main(new String[] {
            testname,
            "filtertrace=true",
            "haltOnError=false",
            "haltOnFailure=false",
            "showoutput=true",
            "outputtoformatters=true",
            "logfailedtests=true",
            "logtestlistenerevents=true",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+testname+".xml" } );
    }

}

