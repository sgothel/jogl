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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.fixedfunc.GLPointerFunc;
import com.jogamp.opengl.glu.GLU;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil.WindowClosingListener;
import com.jogamp.opengl.util.Animator;


/**
 * TestSharedContextNewtAWTBug523
 *
 * Opens a single JFrame with two OpenGL windows and sliders to adjust the view orientation.
 *
 * Each window renders a red triangle and a blue triangle.
 * The red triangle is rendered using glBegin / glVertex / glEnd.
 * The blue triangle is rendered using vertex buffer objects.
 *
 * VAO's are not used to allow testing on OSX GL2 context!
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
 * {@link TestSharedContextVBOES2NEWT1} and {@link TestSharedContextVBOES1NEWT}.
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
    private static AtomicInteger sharedVertexBufferObjects = new AtomicInteger(0);
    private static AtomicInteger sharedIndexBufferObjects = new AtomicInteger(0);

    @BeforeClass
    public static void initClass() {
        if(!GLProfile.isAvailable(GLProfile.GL2)) {
            setTestSupported(false);
        }
    }

    static private GLOffscreenAutoDrawable initShared(final GLCapabilities caps) {
        final GLOffscreenAutoDrawable sharedDrawable = GLDrawableFactory.getFactory(caps.getGLProfile()).createOffscreenAutoDrawable(null, caps, null, 64, 64);
        Assert.assertNotNull(sharedDrawable);
        // init and render one frame, which will setup the Gears display lists
        sharedDrawable.display();
        final GLContext ctx = sharedDrawable.getContext();
        Assert.assertNotNull("Shared drawable's ctx is null", ctx);
        Assert.assertTrue("Shared drawable's ctx is not created", ctx.isCreated());
        return sharedDrawable;
    }

    static private void releaseShared(final GLOffscreenAutoDrawable sharedDrawable) {
        if(null != sharedDrawable) {
            sharedDrawable.destroy();
        }
    }

    // inner class that implements the event listener
    static class TwoTriangles implements GLEventListener {

        boolean useShared;
        int canvasWidth;
        int canvasHeight;
        private static final float boundsRadius = 2f;
        private float viewDistance;
        private static float viewDistanceFactor = 1.0f;
        private float xAxisRotation;
        private float yAxisRotation;
        private static final float viewFovDegrees = 15f;

        // Buffer objects can be shared across canvas instances, if those canvases are initialized with the same GLContext.
        // If we run with sharedBufferObjects true, then the tests will use these shared buffer objects.
        // If we run with sharedBufferObjects false, then each event listener allocates its own buffer objects.
        private final int [] privateVertexBufferObjects = {0};
        private final int [] privateIndexBufferObjects = {0};

        public static int createVertexBuffer(final GL2 gl2) {
            final FloatBuffer vertexBuffer = Buffers.newDirectFloatBuffer(18);
            vertexBuffer.put(new float[]{
                    1.0f, -0.5f, 0f,    // vertex 1
                    0f, 0f, 1f,         // normal 1
                    1.5f, -0.5f, 0f,    // vertex 2
                    0f, 0f, 1f,         // normal 2
                    1.0f, 0.5f, 0f,     // vertex 3
                    0f, 0f, 1f          // normal 3
            });
            vertexBuffer.position(0);

            final int[] vbo = { 0 };
            gl2.glGenBuffers(1, vbo, 0);
            gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
            gl2.glBufferData(GL.GL_ARRAY_BUFFER, vertexBuffer.capacity() * Buffers.SIZEOF_FLOAT, vertexBuffer, GL.GL_STATIC_DRAW);
            gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            return vbo[0];
        }
        public static int createVertexIndexBuffer(final GL2 gl2) {
            final IntBuffer indexBuffer = Buffers.newDirectIntBuffer(3);
            indexBuffer.put(new int[]{0, 1, 2});
            indexBuffer.position(0);

            final int[] vbo = { 0 };
            gl2.glGenBuffers(1, vbo, 0);
            gl2.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vbo[0]);
            gl2.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * Buffers.SIZEOF_INT, indexBuffer, GL.GL_STATIC_DRAW);
            gl2.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
            return vbo[0];
        }

        TwoTriangles (final int canvasWidth, final int canvasHeight, final boolean useShared) {
            // instanceNum = instanceCounter++;
            this.canvasWidth = canvasWidth;
            this.canvasHeight = canvasHeight;
            this.useShared = useShared;
        }

        public void setXAxisRotation(final float xRot) {
            xAxisRotation = xRot;
        }

        public void setYAxisRotation(final float yRot) {
            yAxisRotation = yRot;
        }

        public void setViewDistanceFactor(final float factor) {
            viewDistanceFactor = factor;
        }


        public void init(final GLAutoDrawable drawable) {
            final GL2 gl2 = drawable.getGL().getGL2();

            System.err.println("INIT GL IS: " + gl2.getClass().getName());

            // Disable VSync
            gl2.setSwapInterval(0);

            // Setup the drawing area and shading mode
            gl2.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

            // the first instance of TwoTriangles initializes the shared buffer objects;
            // synchronizing to deal with potential liveness issues if the data is initialized from one thread and used on another
            synchronized (this) {
                // use either the shared or private vertex buffers, as
                int [] vertexBufferObjects;
                int [] indexBufferObjects;
                //
                if (useShared) {
                    System.err.println("Using shared VBOs on slave 0x"+Integer.toHexString(hashCode()));
                    privateVertexBufferObjects[0] = sharedVertexBufferObjects.get();
                    privateIndexBufferObjects[0] = sharedIndexBufferObjects.get();
                } else {
                    System.err.println("Using local VBOs on slave 0x"+Integer.toHexString(hashCode()));
                }
                vertexBufferObjects = privateVertexBufferObjects;
                indexBufferObjects = privateIndexBufferObjects;

                // if buffer sharing is enabled, then the first of the two event listeners to be
                // initialized will allocate the buffers, and the other will re-use the allocated one
                if (vertexBufferObjects[0] == 0) {
                    System.err.println("Creating vertex VBO on slave 0x"+Integer.toHexString(hashCode()));
                    vertexBufferObjects[0] = createVertexBuffer(gl2);
                    if (useShared) {
                        sharedVertexBufferObjects.set(vertexBufferObjects[0]);
                    }
                }

                // A check in the case that buffer sharing is enabled but context sharing is not enabled -- in that
                // case, the buffer objects are not shareable, and the blue triangle cannot be rendereds.
                // Furthermore, although the calls to bind and draw elements do not cause an error from glGetError
                // when this check is removed, true blue triangle is not rendered anyways, and more importantly,
                // I found that with my system glDrawElements causes a runtime exception 50% of the time. Executing the binds
                // to unshareable buffers sets up glDrawElements for unpredictable crashes -- sometimes it does, sometimes not.
                if (gl2.glIsBuffer(vertexBufferObjects[0])) {
                    gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexBufferObjects[0]);
                    //
                    gl2.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
                    gl2.glVertexPointer(3, GL.GL_FLOAT, 6 * Buffers.SIZEOF_FLOAT, 0);
                    //
                    gl2.glEnableClientState(GLPointerFunc.GL_NORMAL_ARRAY);
                    gl2.glNormalPointer(GL.GL_FLOAT, 6 * Buffers.SIZEOF_FLOAT, 3 * Buffers.SIZEOF_FLOAT);
                } else {
                    System.err.println("Vertex VBO is not a buffer on slave 0x"+Integer.toHexString(hashCode()));
                }

                if (indexBufferObjects[0] == 0) {
                    System.err.println("Creating index VBO on slave 0x"+Integer.toHexString(hashCode()));
                    indexBufferObjects[0] = createVertexIndexBuffer(gl2);
                    if (useShared) {
                        sharedIndexBufferObjects.set(indexBufferObjects[0]);
                    }
                }

                // again, a check in the case that buffer sharing is enabled but context sharing is not enabled
                if (gl2.glIsBuffer(indexBufferObjects[0])) {
                    gl2.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, indexBufferObjects[0]);
                } else {
                    System.err.println("Index VBO is not a buffer on slave 0x"+Integer.toHexString(hashCode()));
                }

                gl2.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
                gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
                gl2.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
                gl2.glDisableClientState(GLPointerFunc.GL_NORMAL_ARRAY);

                initializationCounter++;
            } // synchronized (this)


            viewDistance = setupViewFrustum(gl2, canvasWidth, canvasHeight, boundsRadius, 1.0f, viewFovDegrees);

        }

        public void dispose(final GLAutoDrawable drawable) {

            synchronized (this) {
                initializationCounter--;

                final GL2 gl2 = drawable.getGL().getGL2();

                // release shared resources
                if (initializationCounter == 0 || !useShared) {
                    // use either the shared or private vertex buffers, as
                    int [] vertexBufferObjects;
                    int [] indexBufferObjects;
                    if (useShared) {
                        privateVertexBufferObjects[0] = sharedVertexBufferObjects.get();
                        privateIndexBufferObjects[0] = sharedIndexBufferObjects.get();
                        sharedVertexBufferObjects.set(0);
                        sharedIndexBufferObjects.set(0);
                    }
                    vertexBufferObjects = privateVertexBufferObjects;
                    indexBufferObjects = privateIndexBufferObjects;

                    gl2.glDeleteBuffers(1, vertexBufferObjects, 0);
                    logAnyErrorCodes(this, gl2, "dispose.2");
                    gl2.glDeleteBuffers(1, indexBufferObjects, 0);
                    logAnyErrorCodes(this, gl2, "dispose.3");
                    vertexBufferObjects[0] = 0;
                    indexBufferObjects[0] = 0;
                }

                // release the main thread once for each disposal
                disposalCompleteSemaphore.release();
            } // synchronized (this)
        }

        public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        }

        public void display(final GLAutoDrawable drawable) {

            // wait until all instances are initialized before attempting to draw using the
            // vertex array object, because the buffers are allocated in init and when the
            // buffers are shared, we need to ensure that they are allocated before trying to use them
            synchronized (this) {
                if (initializationCounter != 2) {
                    return;
                }
            }

            final GL2 gl2 = drawable.getGL().getGL2();
            final GLU glu = new GLU();

            logAnyErrorCodes(this, gl2, "display.0");

            // Clear the drawing area
            gl2.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

            gl2.glViewport(0, 0, canvasWidth, canvasHeight);
            gl2.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            gl2.glLoadIdentity();
            glu.gluPerspective(viewFovDegrees, (float)canvasWidth/(float)canvasHeight,
                               viewDistance*viewDistanceFactor-boundsRadius, viewDistance*viewDistanceFactor+boundsRadius);

            // Reset the current matrix to the "identity"
            gl2.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            gl2.glLoadIdentity();

            // draw the scene
            gl2.glPushAttrib(GL2.GL_ALL_ATTRIB_BITS);
            gl2.glPushMatrix();

            glu.gluLookAt(0, 0, 0 + viewDistance*viewDistanceFactor, 0, 0, 0, 0, 1, 0);
            gl2.glRotatef(xAxisRotation, 1, 0, 0);
            gl2.glRotatef(yAxisRotation, 0, 1, 0);

            gl2.glDisable(GL.GL_CULL_FACE);
            gl2.glEnable(GL.GL_DEPTH_TEST);

            logAnyErrorCodes(this, gl2, "display.1");

            // draw the triangles
            drawTwoTriangles(gl2);

            gl2.glPopMatrix();
            gl2.glPopAttrib();

            // Flush all drawing operations to the graphics card
            gl2.glFlush();

            logAnyErrorCodes(this, gl2, "display.X");
        }

        public void drawTwoTriangles(final GL2 gl2) {

            // draw a red triangle the old fashioned way
            gl2.glColor3f(1f, 0f, 0f);
            gl2.glBegin(GL.GL_TRIANGLES);
            gl2.glVertex3d(-1.5, -0.5, 0);
            gl2.glNormal3d(0, 0, 1);
            gl2.glVertex3d(-0.5, -0.5, 0);
            gl2.glNormal3d(0, 0, 1);
            gl2.glVertex3d(-0.75, 0.5, 0);
            gl2.glNormal3d(0, 0, 1);
            gl2.glEnd();

            logAnyErrorCodes(this, gl2, "drawTwoTriangles.1");

            // draw the blue triangle using a vertex array object, sharing the vertex and index buffer objects across
            // contexts; if context sharing is not initialized, then one window will simply have to live without a blue triangle
            //
            // synchronizing to deal with potential liveness issues if the data is initialized from one
            // thread and used on another
            boolean vboBound = false;
            // use either the shared or private vertex buffers, as
            int [] vertexBufferObjects;
            int [] indexBufferObjects;
            synchronized (this) {
                if (useShared) {
                    privateVertexBufferObjects[0] = sharedVertexBufferObjects.get();
                    privateIndexBufferObjects[0] = sharedIndexBufferObjects.get();
                }
                vertexBufferObjects = privateVertexBufferObjects;
                indexBufferObjects = privateIndexBufferObjects;
            } // synchronized (this)

            // A check in the case that buffer sharing is enabled but context sharing is not enabled -- in that
            // case, the buffer objects are not shareable, and the blue triangle cannot be rendereds.
            // Furthermore, although the calls to bind and draw elements do not cause an error from glGetError
            // when this check is removed, true blue triangle is not rendered anyways, and more importantly,
            // I found that with my system glDrawElements causes a runtime exception 50% of the time. Executing the binds
            // to unshareable buffers sets up glDrawElements for unpredictable crashes -- sometimes it does, sometimes not.
            final boolean isVBO1 = gl2.glIsBuffer(indexBufferObjects[0]);
            final boolean isVBO2 = gl2.glIsBuffer(vertexBufferObjects[0]);
            final boolean useVBO = isVBO1 && isVBO2;
            if ( useVBO ) {
                gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, vertexBufferObjects[0]);
                gl2.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, indexBufferObjects[0]);

                gl2.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
                // gl2.glVertexPointer(3, GL2.GL_FLOAT, 6 * GLBuffers.SIZEOF_FLOAT, 0);
                gl2.glEnableClientState(GLPointerFunc.GL_NORMAL_ARRAY);
                // gl2.glNormalPointer(GL2.GL_FLOAT, 6 * GLBuffers.SIZEOF_FLOAT, 3 * GLBuffers.SIZEOF_FLOAT);
                vboBound = true;
            }
            // System.err.println("XXX VBO bound "+vboBound+"[ vbo1 "+isVBO1+", vbo2 "+isVBO2+"]");

            logAnyErrorCodes(this, gl2, "drawTwoTriangles.2");

            if (vboBound) {
                gl2.glColor3f(0f, 0f, 1f);
                gl2.glDrawElements(GL.GL_TRIANGLES, 3, GL.GL_UNSIGNED_INT, 0);
                gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
                gl2.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
                gl2.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
                gl2.glDisableClientState(GLPointerFunc.GL_NORMAL_ARRAY);
            }

            logAnyErrorCodes(this, gl2, "drawTwoTriangles.3");
        }

        public void displayChanged(final GLAutoDrawable drawable, final boolean modeChanged, final boolean deviceChanged) {
        }

    } // inner class TwoTriangles

    private static final Set<String> errorSet = new HashSet<String>();

    public static void logAnyErrorCodes(final Object obj, final GL gl, final String prefix) {
        final int glError = gl.glGetError();
        if(glError != GL.GL_NO_ERROR) {
            final String errStr = "GL-Error: "+prefix + " on obj 0x"+Integer.toHexString(obj.hashCode())+", OpenGL error: 0x" + Integer.toHexString(glError);
            if( errorSet.add(errStr) ) {
                System.err.println(errStr);
                ExceptionUtils.dumpStack(System.err);
            }
        }
        final int status = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
        if (status != GL.GL_FRAMEBUFFER_COMPLETE) {
            final String errStr = "GL-Error: "+prefix + " on obj 0x"+Integer.toHexString(obj.hashCode())+", glCheckFramebufferStatus: 0x" + Integer.toHexString(status);
            if( errorSet.add(errStr) ) {
                System.err.println(errStr);
                ExceptionUtils.dumpStack(System.err);
            }
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
    public static float setupViewFrustum(final GL2 gl2, final int width, final int height, final float boundsRadius, final float zoomFactor, final float viewFovDegrees) {
    	assert boundsRadius > 0.0f;
    	assert zoomFactor > 0.0f;
    	assert viewFovDegrees > 0.0f;

        final GLU glu = new GLU();

        final float aspectRatio = (float) width / (float) height;
        final float boundRadiusAdjusted = boundsRadius / zoomFactor;
        final float lowestFov = aspectRatio > 1.0f ? viewFovDegrees : aspectRatio * viewFovDegrees;
        final float viewDist = (float) (boundRadiusAdjusted / Math.sin( (lowestFov / 2.0) * (Math.PI / 180.0) ));

        gl2.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl2.glLoadIdentity();
        glu.gluPerspective(viewFovDegrees, aspectRatio,	0.1*viewDist, viewDist + boundRadiusAdjusted);

        return viewDist;
    }

    @Test
    public void test01UseAWTNotShared() throws InterruptedException, InvocationTargetException {
        testContextSharingCreateVisibleDestroy(false, false);
    }

    @Test
    public void test02UseAWTSharedContext() throws InterruptedException, InvocationTargetException {
        testContextSharingCreateVisibleDestroy(false, true);
    }

    @Test
    public void test10UseNEWTNotShared() throws InterruptedException, InvocationTargetException {
        testContextSharingCreateVisibleDestroy(true, false);
    }

    @Test
    public void test11UseNEWTSharedContext() throws InterruptedException, InvocationTargetException {
        testContextSharingCreateVisibleDestroy(true, true);
    }

    /**
     * Assemble the user interface and start the animator.
     * It waits until the window is closed an then attempts orderly shutdown and resource deallocation.
     */
    public void testContextSharingCreateVisibleDestroy(final boolean useNewt, final boolean shareContext) throws InterruptedException, InvocationTargetException {
        final JFrame frame = new JFrame("Simple JOGL App for testing context sharing");
        final WindowClosingListener awtClosingListener = AWTRobotUtil.addClosingListener(frame);

        //
        // GLDrawableFactory factory = GLDrawableFactory.getFactory(GLProfile.get(GLProfile.GL2));
        // GLContext sharedContext = factory.getOrCreateSharedContext(factory.getDefaultDevice());
        //
        final GLCapabilities glCapabilities = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        glCapabilities.setSampleBuffers(true);
        glCapabilities.setNumSamples(4);

        final GLOffscreenAutoDrawable sharedDrawable;
        if(shareContext) {
            sharedDrawable = initShared(glCapabilities);
        } else {
            sharedDrawable = null;
        }

        final TwoTriangles eventListener1 = new TwoTriangles(640, 480, shareContext);
        final TwoTriangles eventListener2 = new TwoTriangles(320, 480, shareContext);

        final Component openGLComponent1;
        final Component openGLComponent2;
        final GLAutoDrawable openGLAutoDrawable1;
        final GLAutoDrawable openGLAutoDrawable2;

        if (useNewt) {
            final GLWindow glWindow1 = GLWindow.create(glCapabilities);
            if(shareContext) {
                glWindow1.setSharedAutoDrawable(sharedDrawable);
            }
            final NewtCanvasAWT newtCanvasAWT1 = new NewtCanvasAWT(glWindow1);
            newtCanvasAWT1.setPreferredSize(new Dimension(eventListener1.canvasWidth, eventListener1.canvasHeight));
            glWindow1.addGLEventListener(eventListener1);
            //
            final GLWindow glWindow2 = GLWindow.create(glCapabilities);
            if(shareContext) {
                glWindow2.setSharedAutoDrawable(sharedDrawable);
            }
            final NewtCanvasAWT newtCanvasAWT2 = new NewtCanvasAWT(glWindow2);
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
                canvas1 = new GLCanvas(glCapabilities);
                canvas1.setSharedAutoDrawable(sharedDrawable);
                canvas2 = new GLCanvas(glCapabilities);
                canvas2.setSharedAutoDrawable(sharedDrawable);
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
        final JSlider xAxisRotationSlider = new JSlider(SwingConstants.VERTICAL, -180, 180, 1);
        xAxisRotationSlider.setPaintTicks(false);
        xAxisRotationSlider.setPaintLabels(false);
        xAxisRotationSlider.setSnapToTicks(false);
        xAxisRotationSlider.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                eventListener1.setXAxisRotation(xAxisRotationSlider.getValue());
                eventListener2.setXAxisRotation(xAxisRotationSlider.getValue());
            }
        });
        final JLabel xAxisRotationLabel = new JLabel("X-Axis Rotation");

        // Create slider for y rotation.
        // The horizontally oriented slider rotates around the y axis
        final JSlider yAxisRotationSlider = new JSlider(SwingConstants.HORIZONTAL, -180, 180, 1);
        yAxisRotationSlider.setPaintTicks(false);
        yAxisRotationSlider.setPaintLabels(false);
        yAxisRotationSlider.setSnapToTicks(false);
        yAxisRotationSlider.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                eventListener1.setYAxisRotation(yAxisRotationSlider.getValue());
                eventListener2.setYAxisRotation(yAxisRotationSlider.getValue());
            }
        });
        final JLabel yAxisRotationLabel = new JLabel("Y-Axis Rotation");

        // Create slider for view distance factor.
        // We want a range of 0.0 to 10.0 with 0.1 increments (so we can scale down using 0.0 to 1.0).
        // So, set JSlider to 0 to 100 and divide by 10.0 in stateChanged
        final JSlider viewDistanceFactorSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 10);
        viewDistanceFactorSlider.setPaintTicks(false);
        viewDistanceFactorSlider.setPaintLabels(false);
        viewDistanceFactorSlider.setSnapToTicks(false);
        viewDistanceFactorSlider.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent e) {
                eventListener1.setViewDistanceFactor(viewDistanceFactorSlider.getValue() / 10.0f);
                eventListener2.setViewDistanceFactor(viewDistanceFactorSlider.getValue() / 10.0f);
            }
        });
        final JLabel viewDistanceFactorLabel = new JLabel("View Distance Factor");

        // group the view distance and label into a vertical panel
        final JPanel viewDistancePanel = new JPanel();
        viewDistancePanel.setLayout(new BoxLayout(viewDistancePanel, BoxLayout.PAGE_AXIS));
        viewDistancePanel.add(Box.createVerticalGlue());
        viewDistancePanel.add(viewDistanceFactorSlider);
        viewDistancePanel.add(viewDistanceFactorLabel);
        viewDistancePanel.add(Box.createVerticalGlue());

        // group both OpenGL canvases / windows into a horizontal panel
        final JPanel openGLPanel = new JPanel();
        openGLPanel.setLayout(new BoxLayout(openGLPanel, BoxLayout.LINE_AXIS));
        openGLPanel.add(openGLComponent1);
        openGLPanel.add(Box.createHorizontalStrut(5));
        openGLPanel.add(openGLComponent2);

        // group the open GL panel and the y-axis rotation slider into a vertical panel.
        final JPanel canvasAndYAxisPanel = new JPanel();
        canvasAndYAxisPanel.setLayout(new BoxLayout(canvasAndYAxisPanel, BoxLayout.PAGE_AXIS));
        canvasAndYAxisPanel.add(openGLPanel);
        canvasAndYAxisPanel.add(Box.createVerticalGlue());
        canvasAndYAxisPanel.add(yAxisRotationSlider);
        canvasAndYAxisPanel.add(yAxisRotationLabel);

        // group the X-axis rotation slider and label into a horizontal panel.
        final JPanel xAxisPanel = new JPanel();
        xAxisPanel.setLayout(new BoxLayout(xAxisPanel, BoxLayout.LINE_AXIS));
        xAxisPanel.add(xAxisRotationSlider);
        xAxisPanel.add(xAxisRotationLabel);

        final JPanel mainPanel = (JPanel) frame.getContentPane();
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
            public void windowClosing(final WindowEvent e) {
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
            final boolean windowOpened = windowOpenSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS);
            Assert.assertEquals(true, windowOpened);
        } catch (final InterruptedException e) {
            System.err.println("Closing wait interrupted: " + e.getMessage());
        }
        animator.start();

        // sleep for test duration, then request the window to close, wait for the window to close,s and stop the animation
        try {
            while(animator.isAnimating() && animator.getTotalFPSDuration() < durationPerTest) {
                Thread.sleep(100);
            }
            AWTRobotUtil.closeWindow(frame, true, awtClosingListener);
            final boolean windowClosed = closingSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS);
            Assert.assertEquals(true, windowClosed);
        } catch (final InterruptedException e) {
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
            final boolean windowsDisposed = closingSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS);
            Assert.assertEquals(true, windowsDisposed);
        } catch (final InterruptedException e) {
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
        } catch (final InterruptedException e) {
            System.err.println("Clean exit interrupted: " + e.getMessage());
        }

        Assert.assertEquals(true, disposalSuccesses == 2);

        releaseShared(sharedDrawable);
    }

    static int atoi(final String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(final String[] args) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                if (++i < args.length) {
                    durationPerTest = atoi(args[i]);
                }
            }
        }
        org.junit.runner.JUnitCore.main(TestSharedContextNewtAWTBug523.class.getName());
    }

}

