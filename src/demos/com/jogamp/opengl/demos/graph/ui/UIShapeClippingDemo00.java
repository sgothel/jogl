/**
 * Copyright 2010-2024 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.demos.graph.ui;

import java.io.File;
import java.io.IOException;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.plane.AffineTransform;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.demos.graph.MSAATool;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;

/**
 * Basic UIShape Clipping demo.
 *
 * Action Cursor-Keys:
 * - With Shift  : Move the clipping-rectangle itself
 * - With Control: Resize Left and Bottom Clipping Edge of AABBox
 * - No Modifiers: Resize Right and Top Clipping Edge of AABBox
 */
public class UIShapeClippingDemo00 implements GLEventListener {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;

    static CommandlineOptions options = new CommandlineOptions(1280, 720, Region.VBAA_RENDERING_BIT);

    static final float szw = 1/3f * 0.8f;
    static final float szh = szw * 1f/2f;
    static Rectangle clipRect;

    public static void main(final String[] args) throws IOException {
        final int[] idx = { 0 };
        for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
            if( options.parse(args, idx) ) {
                continue;
            }
        }
        System.err.println(options);
        final GLCapabilities reqCaps = options.getGLCaps();
        System.out.println("Requested: " + reqCaps);

        final GLWindow window = GLWindow.create(reqCaps);
        // window.setPosition(10, 10);
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(UIShapeClippingDemo00.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());

        clipRect = new Rectangle(options.renderModes, szw, szh, 1/2000f);
        clipRect.move(-szw/2f, -szh/2f, 0).setColor(0, 0, 0, 1);

        final UIShapeClippingDemo00 uiGLListener = new UIShapeClippingDemo00(options.renderModes, DEBUG, TRACE);
        uiGLListener.attachInputListenerTo(window);
        window.addGLEventListener(uiGLListener);
        window.setVisible(true);

        final Animator animator = new Animator(0 /* w/o AWT */);
        animator.setUpdateFPSFrames(5*60, null);
        animator.add(window);

        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent arg0) {
                final short keySym = arg0.getKeySymbol();
                final float less = 0.99f;
                final float more = 1.01f;
                final float x = clipRect.getX();
                final float y = clipRect.getY();
                final float z = clipRect.getZ();
                final float w = clipRect.getWidth();
                final float h = clipRect.getHeight();
                if( keySym == KeyEvent.VK_LEFT ) {
                    if( arg0.isShiftDown() ) {
                        final float d = w*more - w;
                        clipRect.move(-d, 0, 0);
                    } else if( arg0.isControlDown() ) {
                        final float d = w*more - w;
                        clipRect.setPosition(x-d, y, z);
                        clipRect.setDimension(w*more, h, clipRect.getLineWidth());
                    } else {
                        clipRect.setDimension(w*less, h, clipRect.getLineWidth());
                    }
                } else if( keySym == KeyEvent.VK_RIGHT ) {
                    if( arg0.isShiftDown() ) {
                        final float d = w*more - w;
                        clipRect.move(d, 0, 0);
                    } else if( arg0.isControlDown() ) {
                        final float d = w - w*less;
                        clipRect.setPosition(x+d, y, z);
                        clipRect.setDimension(w*less, h, clipRect.getLineWidth());
                    } else {
                        clipRect.setDimension(w*more, h, clipRect.getLineWidth());
                    }
                } else if( keySym == KeyEvent.VK_UP ) {
                    if( arg0.isShiftDown() ) {
                        final float d = h*more - h;
                        clipRect.move(0, d, 0);
                    } else if( arg0.isControlDown() ) {
                        final float d = h - h*less;
                        clipRect.setPosition(x, y+d, z);
                        clipRect.setDimension(w, h*less, clipRect.getLineWidth());
                    } else {
                        clipRect.setDimension(w, h*more, clipRect.getLineWidth());
                    }
                } else if( keySym == KeyEvent.VK_DOWN ) {
                    if( arg0.isShiftDown() ) {
                        final float d = h*more - h;
                        clipRect.move(0, -d, 0);
                    } else if( arg0.isControlDown() ) {
                        final float d = h*more - h;
                        clipRect.setPosition(x, y-d, z);
                        clipRect.setDimension(w, h*more, clipRect.getLineWidth());
                    } else {
                        clipRect.setDimension(w, h*less, clipRect.getLineWidth());
                    }
                } else if( keySym == KeyEvent.VK_F4 || keySym == KeyEvent.VK_ESCAPE || keySym == KeyEvent.VK_Q ) {
                    new InterruptSource.Thread( () -> { window.destroy(); } ).start();
                }
            }
        });
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(final WindowEvent e) {
                animator.stop();
            }
        });

        animator.start();
    }

    private final GLReadBufferUtil screenshot;
    private final int renderModes;
    private final RegionRenderer rRenderer;
    private final boolean debug;
    private final boolean trace;

    private Shape shape;

    private KeyAction keyAction;

    private volatile GLAutoDrawable autoDrawable = null;

    private final float[] position = new float[] {0,0,0};

    private static final float xTran = 0f;
    private static final float yTran = 0f;
    private static final float zTran = -1/5f;
    private static final float zNear = 0.1f;
    private static final float zFar = 7000.0f;

    boolean ignoreInput = false;

    protected final AffineTransform tempT1 = new AffineTransform();
    protected final AffineTransform tempT2 = new AffineTransform();

    public UIShapeClippingDemo00(final int renderModes, final boolean debug, final boolean trace) {
        this.renderModes = renderModes;
        this.rRenderer = RegionRenderer.create(RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
        this.rRenderer.setAAQuality(options.graphAAQuality);
        this.rRenderer.setSampleCount(options.graphAASamples);
        this.debug = debug;
        this.trace = trace;
        this.screenshot = new GLReadBufferUtil(false, false);

        this.shape = new Rectangle(renderModes, szw, szh, 0);
        this.shape.move(-szw/2f, -szh/2f, 0);
    }

    public final RegionRenderer getRegionRenderer() { return rRenderer; }
    public final float[] getPosition() { return position; }

    @Override
    public void init(final GLAutoDrawable drawable) {
        autoDrawable = drawable;
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(debug) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", null, gl, null) ).getGL2ES2();
        }
        if(trace) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", null, gl, new Object[] { System.err } ) ).getGL2ES2();
        }
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        getRegionRenderer().init(gl);

        gl.setSwapInterval(1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        // gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
        MSAATool.dump(drawable);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int xstart, final int ystart, final int width, final int height) {
        // final GL2ES2 gl = drawable.getGL().getGL2ES2();
        // gl.glViewport(xstart, ystart, width, height);

        rRenderer.reshapePerspective(FloatUtil.QUARTER_PI, width, height, zNear, zFar);
        // rRenderer.reshapeOrtho(width, height, zNear, zFar);

        final PMVMatrix4f pmv = rRenderer.getMatrix();
        pmv.loadMvIdentity();
        pmv.translateMv(xTran, yTran, zTran);

        if( drawable instanceof Window ) {
            ((Window)drawable).setTitle(UIShapeClippingDemo00.class.getSimpleName()+": "+drawable.getSurfaceWidth()+" x "+drawable.getSurfaceHeight());
        }
    }

    private void drawShape(final GL2ES2 gl, final RegionRenderer renderer, final Shape shape) {
        final PMVMatrix4f pmv = renderer.getMatrix();
        pmv.pushMv();
        if( null != shape && shape.isVisible() ) {
            shape.setTransformMv(pmv);
            shape.draw(gl, renderer);
        }
        pmv.popMv();
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final RegionRenderer renderer = getRegionRenderer();
        renderer.enable(gl, true);
        {
            drawShape(gl, renderer, clipRect);
            {
                final AABBox clipBBox; // Mv pre-multiplied AABBox
                {
                    final PMVMatrix4f pmv = renderer.getMatrix();
                    pmv.pushMv();
                    clipRect.setTransformMv(pmv);
                    clipBBox = clipRect.getBounds().transform(pmv.getMv(), new AABBox());
                    pmv.popMv();
                }
                renderer.setClipBBox( clipBBox );
                // System.err.println("Clipping "+renderer.getClipBBox());
                drawShape(gl, renderer, shape);
                // System.err.println("draw.0: "+shape);
                renderer.setClipBBox(null);
            }
        }
        renderer.enable(gl, false);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        if( null != shape ) {
            shape.destroy(gl, getRegionRenderer());
            shape = null;
        }
        if( null != clipRect ) {
            clipRect.destroy(gl, getRegionRenderer());
        }
        autoDrawable = null;
        screenshot.dispose(gl);
        rRenderer.destroy(gl);
    }

    /** Attach the input listener to the window */
    public void attachInputListenerTo(final GLWindow window) {
        if ( null == keyAction ) {
            keyAction = new KeyAction();
            window.addKeyListener(keyAction);
        }
    }

    public void detachFrom(final GLWindow window) {
        if ( null == keyAction ) {
            return;
        }
        window.removeGLEventListener(this);
        window.removeKeyListener(keyAction);
    }

    public void printScreen(final GLAutoDrawable drawable, final String dir, final String tech, final String objName, final boolean exportAlpha) throws GLException, IOException {
        final String sw = String.format("-%03dx%03d-Z%04d-T%04d-%s", drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), (int)Math.abs(zTran), 0, objName);

        final String filename = dir + tech + sw +".png";
        if(screenshot.readPixels(drawable.getGL(), false)) {
            screenshot.write(new File(filename));
        }
    }

    int screenshot_num = 0;

    public void setIgnoreInput(final boolean v) {
        ignoreInput = v;
    }
    public boolean getIgnoreInput() {
        return ignoreInput;
    }

    public class KeyAction implements KeyListener {
        @Override
        public void keyPressed(final KeyEvent arg0) {
            if(ignoreInput) {
                return;
            }

            if(arg0.getKeyCode() == KeyEvent.VK_S){
                if(null != autoDrawable) {
                    autoDrawable.invoke(false, new GLRunnable() {
                        @Override
                        public boolean run(final GLAutoDrawable drawable) {
                            try {
                                final String type = Region.getRenderModeString(renderModes);
                                printScreen(drawable, "./", "demo-"+type, "snap"+screenshot_num, false);
                                screenshot_num++;
                            } catch (final GLException e) {
                                e.printStackTrace();
                            } catch (final IOException e) {
                                e.printStackTrace();
                            }
                            return true;
                        }
                    });
                }
            }
        }
        @Override
        public void keyReleased(final KeyEvent arg0) {}
    }
}
