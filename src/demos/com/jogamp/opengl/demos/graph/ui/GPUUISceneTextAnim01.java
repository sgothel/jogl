/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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
import java.util.Arrays;
import java.util.Locale;

import com.jogamp.common.util.IOUtil;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontScale;
import com.jogamp.graph.ui.gl.Scene;
import com.jogamp.graph.ui.gl.Shape;
import com.jogamp.graph.ui.gl.shapes.Label;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.demos.graph.FontSetDemos;
import com.jogamp.opengl.demos.graph.MSAATool;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.GLReadBufferUtil;

public class GPUUISceneTextAnim01 implements GLEventListener {

    private boolean debug = false;
    private boolean trace = false;

    private final float noAADPIThreshold;
    private final Scene scene;

    /** -1 == AUTO, TBD @ init(..) */
    private int renderModes;

    private final Font font;
    private final Font fontFPS;

    // private final float relTop = 80f/100f;
    private final float relMiddle = 22f/100f;
    // private final float relLeft = 11f/100f;

    private final float fontSizePt = 10f;
    /** Proportional Font Size to Window Height  for Main Text, per-vertical-pixels [PVP] */
    private final float fontSizeFixedPVP = 0.04f;
    /** Proportional Font Size to Window Height for FPS Status Line, per-vertical-pixels [PVP] */
    private final float fontSizeFpsPVP = 0.03f;
    private float dpiV = 96;

    /**
     * Default DPI threshold value to disable {@link Region#VBAA_RENDERING_BIT VBAA}: {@value} dpi
     * @see #GPUUISceneGLListener0A(float)
     * @see #GPUUISceneGLListener0A(float, boolean, boolean)
     */
    public static final float DefaultNoAADPIThreshold = 200f;

    private String actionText = null;
    private Label jogampLabel = null;
    private Label fpsLabel = null;

    // private GLAutoDrawable cDrawable;

    private final GLReadBufferUtil screenshot;

    private final String jogamp = "JogAmp - Jogl Graph Module Demo";

    // private final String longText = "JOGL: Java™ Binding for the OpenGL® API.";

    public GPUUISceneTextAnim01(final String fontfilename, final float noAADPIThreshold, final int renderModes, final boolean debug, final boolean trace) {
        this.noAADPIThreshold = noAADPIThreshold;
        this.debug = debug;
        this.trace = trace;

        this.renderModes = renderModes;

        try {
            if( null == fontfilename ) {
                font = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerif.ttf",
                                       FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
            } else {
                font = FontFactory.get( new File( fontfilename ) );
            }
            System.err.println("Font "+font.getFullFamilyName());

            fontFPS = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeMonoBold.ttf",
                                      FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
            System.err.println("Font FPS "+fontFPS.getFullFamilyName());

        } catch (final IOException ioe) {
            throw new RuntimeException(ioe);
        }

        scene = new Scene();
        screenshot = new GLReadBufferUtil(false, false);
    }

    private void setupUI(final GLAutoDrawable drawable) {
        final float pixelSizeFixed = fontSizeFixedPVP * drawable.getSurfaceHeight();
        jogampLabel = new Label(scene.getVertexFactory(), renderModes, font, pixelSizeFixed, jogamp);
        jogampLabel.addMouseListener(dragZoomRotateListener);
        scene.addShape(jogampLabel);
        jogampLabel.setEnabled(true);

        final float pixelSize10Pt = FontScale.toPixels(fontSizePt, dpiV);
        System.err.println("10Pt PixelSize: Display "+dpiV+" dpi, fontSize "+fontSizePt+" ppi -> "+pixelSize10Pt+" pixel-size");

        /**
         *
         * [Label] Display 112.88889 dpi, fontSize 12.0 ppi -> pixelSize 18.814816
         * [FPS] Display 112.88889 dpi, fontSize 12.0 ppi -> pixelSize 15.679012
         */
        final float pixelSizeFPS = fontSizeFpsPVP * drawable.getSurfaceHeight();
        fpsLabel = new Label(scene.getVertexFactory(), renderModes, fontFPS, pixelSizeFPS, "Nothing there yet");
        fpsLabel.addMouseListener(dragZoomRotateListener);
        scene.addShape(fpsLabel);
        fpsLabel.setEnabled(true);
        fpsLabel.setColor(0.1f, 0.1f, 0.1f, 1.0f);
        fpsLabel.move(0f, pixelSizeFPS * (fontFPS.getMetrics().getLineGap() - fontFPS.getMetrics().getDescent()), 0f);
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        final Object upObj = drawable.getUpstreamWidget();
        if( upObj instanceof Window ) {
            final Window upWin = (Window)upObj;
            final MonitorDevice monitor = upWin.getMainMonitor();
            final float[] monitorDPI = MonitorDevice.perMMToPerInch( monitor.getPixelsPerMM(new float[2]) );
            final float[] sDPI = MonitorDevice.perMMToPerInch( upWin.getPixelsPerMM(new float[2]) );
            dpiV = sDPI[1];
            System.err.println("Monitor detected: "+monitor);
            System.err.println("Monitor dpi: "+monitorDPI[0]+" x "+monitorDPI[1]);
            System.err.println("Surface scale: native "+Arrays.toString(upWin.getMaximumSurfaceScale(new float[2]))+", current "+Arrays.toString(upWin.getCurrentSurfaceScale(new float[2])));
            System.err.println("Surface dpi "+sDPI[0]+" x "+sDPI[1]);
        } else {
            System.err.println("Using default DPI of "+dpiV);
        }
        if( 0 == renderModes && !FloatUtil.isZero(noAADPIThreshold, FloatUtil.EPSILON)) {
            final boolean noAA = dpiV >= noAADPIThreshold;
            final String noAAs = noAA ? " >= " : " < ";
            System.err.println("AUTO RenderMode: dpi "+dpiV+noAAs+noAADPIThreshold+" -> noAA "+noAA);
            renderModes = noAA ? 0 : Region.VBAA_RENDERING_BIT;
        }
        if(drawable instanceof GLWindow) {
            System.err.println("GPUUISceneGLListener0A: init (1)");
            final GLWindow glw = (GLWindow) drawable;
            scene.attachInputListenerTo(glw);
        } else {
            System.err.println("GPUUISceneGLListener0A: init (0)");
        }
        // cDrawable = drawable;
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(debug) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", null, gl, null) ).getGL2ES2();
        }
        if(trace) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", null, gl, new Object[] { System.err } ) ).getGL2ES2();
        }
        System.err.println(JoglVersion.getGLInfo(gl, null, false /* withCapsAndExts */).toString());
        System.err.println("VSync Swap Interval: "+gl.getSwapInterval());
        System.err.println("Chosen: "+drawable.getChosenGLCapabilities());
        MSAATool.dump(drawable);

        gl.setSwapInterval(1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_BLEND);

        scene.init(drawable);

        final GLAnimatorControl a = drawable.getAnimator();
        if( null != a ) {
            a.resetFPSCounter();
        }

        setupUI(drawable);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        System.err.println("GPUUISceneGLListener0A: reshape");

        //
        // Layout all shapes: Relational move regarding window coordinates
        //
        final float dz = 0f;

        final float dxMiddleAbs = width * relMiddle;
        final float dyTopLabelAbs = drawable.getSurfaceHeight() - 2f*jogampLabel.getLineHeight();
        jogampLabel.setPosition(dxMiddleAbs, dyTopLabelAbs, dz);
        fpsLabel.move(0f, 0f, 0f);

        scene.reshape(drawable, x, y, width, height);

        lastWidth = width;
        lastHeight = height;
    }
    float lastWidth = 0f, lastHeight = 0f;

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        System.err.println("GPUUISceneGLListener0A: dispose");

        scene.dispose(drawable); // disposes all registered UIShapes

        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        screenshot.dispose(gl);
    }

    private int shotCount = 0;

    public void printScreen(final GL gl)  {
        final RegionRenderer renderer = scene.getRenderer();
        final String modeS = Region.getRenderModeString(jogampLabel.getRenderModes());
        final String filename = String.format((Locale)null, "GraphUIDemo-shot%03d-%03dx%03d-S_%s_%02d.png",
                shotCount++, renderer.getWidth(), renderer.getHeight(),
                modeS, scene.getSampleCount());
        gl.glFinish(); // just make sure rendering finished ..
        if(screenshot.readPixels(gl, false)) {
            screenshot.write(new File(filename));
            System.err.println("Wrote: "+filename);
        }
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        if( fpsLabel.isEnabled() ) {
            final String text;
            if( null == actionText ) {
                text = scene.getStatusText(drawable, renderModes, fpsLabel.getQuality(), dpiV);
            } else if( null != drawable.getAnimator() ) {
                text = Scene.getStatusText(drawable.getAnimator())+", "+actionText;
            } else {
                text = actionText;
            }
            if( fpsLabel.setText(text) ) { // marks dirty only if text differs.
                System.err.println(text);
            }
        }
        scene.display(drawable);
    }

    public void attachInputListenerTo(final GLWindow window) {
        scene.attachInputListenerTo(window);
    }

    public void detachInputListenerFrom(final GLWindow window) {
        scene.detachInputListenerFrom(window);
    }

    /**
     * We can share this instance w/ all UI elements,
     * since only mouse action / gesture is complete for a single one (press, drag, released and click).
     */
    private final Shape.MouseGestureAdapter dragZoomRotateListener = new Shape.MouseGestureAdapter() {
        @Override
        public void mouseReleased(final MouseEvent e) {
            actionText = null;
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
            if( e.getPointerCount() == 1 ) {
                final float[] tx = shapeEvent.shape.getPosition();
                actionText = String.format((Locale)null, "Pos %6.2f / %6.2f / %6.2f", tx[0], tx[1], tx[2]);
            }
        }
    };
}
