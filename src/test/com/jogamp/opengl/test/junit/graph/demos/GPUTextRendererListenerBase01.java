/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.graph.demos;

import java.io.IOException;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLException;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.PMVMatrix;

/**
 *
 * GPURendererListenerBase01 Keys:
 * - 1/2: zoom in/out
 * - 6/7: 2nd pass texture size
 * - 0/9: rotate
 * - v: toggle v-sync
 * - s: screenshot
 *
 * Additional Keys:
 * - 3/4: font +/-
 * - h: toogle draw 'font set'
 * - f: toggle draw fps
 * - space: toggle font (ubuntu/java)
 * - i: live input text input (CR ends it, backspace supported)
 */
public abstract class GPUTextRendererListenerBase01 extends GPURendererListenerBase01 {
    public final TextRegionUtil textRegionUtil;
    private final GLRegion regionFPS;
    private final boolean useBlending;
    int fontSet = FontFactory.UBUNTU;
    Font font;

    int headType = 0;
    boolean drawFPS = true;
    final float fontSizeFName = 8f;
    final float fontSizeFPS = 10f;
    final int[] sampleCountFPS = new int[] { 8 };
    float fontSizeHead = 12f;
    float fontSizeBottom = 16f;
    float dpiH = 96;
    final int fontSizeModulo = 100;
    String fontName;
    AABBox fontNameBox;
    String headtext;
    AABBox headbox;

    static final String text1 = "abcdefghijklmnopqrstuvwxyz\nABCDEFGHIJKLMNOPQRSTUVWXYZ\n0123456789.:,;(*!?/\\\")$%^&-+@~#<>{}[]";
    static final String text2 = "The quick brown fox jumps over the lazy dog";
    static final String textX =
        "JOGAMP graph demo using Resolution Independent NURBS\n"+
        "JOGAMP JOGL - OpenGL ES2 profile\n"+
        "Press 1/2 to zoom in/out the below text\n"+
        "Press 3/4 to incr/decs font size (alt: head, w/o bottom)\n"+
        "Press 6/7 to edit texture size if using VBAA\n"+
        "Press 0/9 to rotate the below string\n"+
        "Press v to toggle vsync\n"+
        "Press i for live input text input (CR ends it, backspace supported)\n"+
        "Press f to toggle fps. H for different text, space for font type\n";

    static final String textX2 =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec nec sapien tellus. \n"+
        "Ut purus odio, rhoncus sit amet commodo eget, ullamcorper vel urna. Mauris ultricies \n"+
        "quam iaculis urna cursus ornare. Nullam ut felis a ante ultrices ultricies nec a elit. \n"+
        "In hac habitasse platea dictumst. Vivamus et mi a quam lacinia pharetra at venenatis est.\n"+
        "Morbi quis bibendum nibh. Donec lectus orci, sagittis in consequat nec, volutpat nec nisi.\n"+
        "Donec ut dolor et nulla tristique varius. In nulla magna, fermentum id tempus quis, semper \n"+
        "in lorem. Maecenas in ipsum ac justo scelerisque sollicitudin. Quisque sit amet neque lorem,\n" +
        "-------Press H to change text---------";

    StringBuilder userString = new StringBuilder();
    boolean userInput = false;
    public GPUTextRendererListenerBase01(RenderState rs, int renderModes, int sampleCount, boolean blending, boolean debug, boolean trace) {
        // NOTE_ALPHA_BLENDING: We use alpha-blending
        super(RegionRenderer.create(rs, renderModes,
                                    blending ? RegionRenderer.defaultBlendEnable : null,
                                    blending ? RegionRenderer.defaultBlendDisable : null),
                                    renderModes, debug, trace);
        this.useBlending = blending;
        this.textRegionUtil = new TextRegionUtil(this.getRenderer());
        this.regionFPS = GLRegion.create(renderModes);
        try {
            this.font = FontFactory.get(fontSet).getDefault();
            dumpFontNames();

            this.fontName = font.toString();
        } catch (IOException ioe) {
            System.err.println("Catched: "+ioe.getMessage());
            ioe.printStackTrace();
        }
        setMatrix(0, 0, 0, 0f, sampleCount);
    }

    void dumpFontNames() {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.err.println(font.getAllNames(null, "\n"));
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }

    void switchHeadBox() {
        headType = ( headType + 1 ) % 4 ;
        switch(headType) {
          case 0:
              headtext = null;
              break;

          case 1:
              headtext= textX2;
              break;
          case 2:
              headtext= textX;
              break;

          default:
              headtext = text1;
        }
        if(null != headtext) {
            headbox = font.getStringBounds(headtext, font.getPixelSize(fontSizeHead, dpiH));
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        super.init(drawable);
        final Object upObj = drawable.getUpstreamWidget();
        if( upObj instanceof Window ) {
            final float[] pixelsPerMM = new float[2];
            ((Window)upObj).getMainMonitor().getPixelsPerMM(pixelsPerMM);
            dpiH = pixelsPerMM[1]*25.4f;
        }
        fontNameBox = font.getStringBounds(fontName, font.getPixelSize(fontSizeFName, dpiH));
        switchHeadBox();

    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        regionFPS.destroy(drawable.getGL().getGL2ES2(), getRenderer());
        super.dispose(drawable);
    }

    public static void mapWin2ObjectCoords(final PMVMatrix pmv, final int[] view,
                                           final float zNear, final float zFar,
                                           float orthoX, float orthoY, float orthoDist,
                                           final float[] winZ, final float[] objPos) {
        winZ[0] = (1f/zNear-1f/orthoDist)/(1f/zNear-1f/zFar);
        pmv.gluUnProject(orthoX, orthoY, winZ[0], view, 0, objPos, 0);
    }
    public static void translateOrtho(final String msg,
                                      final PMVMatrix pmv, final int[] view,
                                      final float zNear, final float zFar,
                                      float orthoX, float orthoY, float orthoDist,
                                      final float[] winZ, final float[] objPos) {
        mapWin2ObjectCoords(pmv, view, zNear, zFar, orthoX, orthoY, orthoDist, winZ, objPos);
        pmv.glTranslatef(objPos[0], objPos[1], objPos[2]);
        /**
        System.err.printf("XXX %7s: [%5.1f, %5.1f, [%5.1f -> %5.1f]] --> [%8.3f, %8.3f, %8.3f]%n",
                msg, orthoX, orthoY, orthoDist, winZ[0], objPos[0], objPos[1], objPos[2]); */
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        final int width = drawable.getWidth();
        final int height = drawable.getHeight();
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final float zDistance0 =   500f;
        final float zDistance1 =   400f;
        final float[] objPos = new float[3];
        final float[] winZ = new float[1];
        final int[] view = new int[] { 0, 0, drawable.getWidth(),  drawable.getHeight() };

        final RegionRenderer renderer = getRenderer();
        final PMVMatrix pmv = renderer.getMatrix();
        renderer.resetModelview(null);
        renderer.setColorStatic(gl, 0.0f, 0.0f, 0.0f);
        if( useBlending ) {
            // NOTE_ALPHA_BLENDING:
            // Due to alpha blending and VBAA, we need a background in text color
            // otherwise blending will amplify 'white'!
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        }

        final float pixelSizeFName = font.getPixelSize(fontSizeFName, dpiH);
        final float pixelSizeHead = font.getPixelSize(fontSizeHead, dpiH);
        final float pixelSizeBottom = font.getPixelSize(fontSizeBottom, dpiH);

        if( drawFPS ) {
            final float pixelSizeFPS = font.getPixelSize(fontSizeFPS, dpiH);
            final float lfps, tfps, td;
            final GLAnimatorControl animator = drawable.getAnimator();
            if( null != animator ) {
                lfps = animator.getLastFPS();
                tfps = animator.getTotalFPS();
                td = animator.getTotalFPSDuration()/1000f;
            } else {
                lfps = 0f;
                tfps = 0f;
                td = 0f;
            }
            final String modeS = Region.getRenderModeString(renderer.getRenderModes());
            final String text = String.format("%03.1f/%03.1f fps, v-sync %d, fontSize [head %.1f, bottom %.1f], %s-samples [%d, this %d], td %4.1f, blend %b, alpha-bits %d",
                    lfps, tfps, gl.getSwapInterval(), fontSizeHead, fontSizeBottom, modeS, getSampleCount()[0], sampleCountFPS[0], td,
                    useBlending, drawable.getChosenGLCapabilities().getAlphaBits());

            // bottom, half line up
            renderer.resetModelview(null);
            translateOrtho("fpstxt", pmv, view, zNear, zFar, 0, pixelSizeFPS/2, zDistance0, winZ, objPos);
            renderer.updateMatrix(gl);

            // No cache, keep region alive!
            TextRegionUtil.drawString3D(regionFPS, renderer, gl, font, pixelSizeFPS, text, sampleCountFPS);
        }

        float dx = width-fontNameBox.getWidth()-2f;
        float dy = height - 10f;

        renderer.resetModelview(null);
        translateOrtho("fontxt", pmv, view, zNear, zFar, dx, dy, zDistance0, winZ, objPos);
        renderer.updateMatrix(gl);
        textRegionUtil.drawString3D(gl, font, pixelSizeFName, fontName, getSampleCount());

        dx  =  10f;
        dy += -fontNameBox.getHeight() - 10f;

        if(null != headtext) {
            renderer.resetModelview(null);
            translateOrtho("headtx", pmv, view, zNear, zFar, dx, dy, zDistance0, winZ, objPos);
            renderer.updateMatrix(gl);
            textRegionUtil.drawString3D(gl, font, pixelSizeHead, headtext, getSampleCount());
        }

        dy += -headbox.getHeight() - font.getLineHeight(pixelSizeBottom);

        renderer.resetModelview(null);
        translateOrtho("Bottom", pmv, view, zNear, zFar, dx, dy, zDistance1, winZ, objPos);
        renderer.translate(null, getXTran(), getYTran(), getZTran());
        renderer.rotate(gl, getAngle(), 0, 1, 0);
        renderer.setColorStatic(gl, 1.0f, 0.0f, 0.0f);
        if( useBlending ) {
            // NOTE_ALPHA_BLENDING:
            // Due to alpha blending and VBAA, we need a background in text color
            // otherwise blending will amplify 'white'!
            gl.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        }

        if(!userInput) {
            textRegionUtil.drawString3D(gl, font, pixelSizeBottom, text2, getSampleCount());
        } else {
            textRegionUtil.drawString3D(gl, font, pixelSizeBottom, userString.toString(), getSampleCount());
        }
    }

    public void fontBottomIncr(int v) {
        fontSizeBottom = Math.abs((fontSizeBottom + v) % fontSizeModulo) ;
        dumpMatrix(true);
    }

    public void fontHeadIncr(int v) {
        fontSizeHead = Math.abs((fontSizeHead + v) % fontSizeModulo) ;
        if(null != headtext) {
            headbox = font.getStringBounds(headtext, font.getPixelSize(fontSizeHead, dpiH));
        }
    }

    public boolean nextFontSet() {
        try {
            int set = ( fontSet == FontFactory.UBUNTU ) ? FontFactory.JAVA : FontFactory.UBUNTU ;
            Font _font = FontFactory.get(set).getDefault();
            if(null != _font) {
                fontSet = set;
                font = _font;
                fontName = font.getFullFamilyName(null).toString();
                fontNameBox = font.getStringBounds(fontName, font.getPixelSize(fontSizeFName, dpiH));
                dumpFontNames();
                return true;
            }
        } catch (IOException ex) {
            System.err.println("Catched: "+ex.getMessage());
        }
        return false;
    }

    public boolean setFontSet(int set, int family, int stylebits) {
        try {
            Font _font = FontFactory.get(set).get(family, stylebits);
            if(null != _font) {
                fontSet = set;
                font = _font;
                fontName = font.getFullFamilyName(null).toString();
                fontNameBox = font.getStringBounds(fontName, font.getPixelSize(fontSizeFName, dpiH));
                dumpFontNames();
                return true;
            }
        } catch (IOException ex) {
            System.err.println("Catched: "+ex.getMessage());
        }
        return false;
    }

    public boolean isUserInputMode() { return userInput; }

    void dumpMatrix(boolean bbox) {
        System.err.println("Matrix: " + getXTran() + "/" + getYTran() + " x"+getZTran() + " @"+getAngle() +" fontSize "+fontSizeBottom);
        if(bbox) {
            System.err.println("bbox: "+font.getStringBounds(text2, font.getPixelSize(fontSizeBottom, dpiH)));
        }
    }

    KeyAction keyAction = null;

    @Override
    public void attachInputListenerTo(GLWindow window) {
        if ( null == keyAction ) {
            keyAction = new KeyAction();
            window.addKeyListener(keyAction);
            super.attachInputListenerTo(window);
        }
    }

    @Override
    public void detachInputListenerFrom(GLWindow window) {
        super.detachInputListenerFrom(window);
        if ( null == keyAction ) {
            return;
        }
        window.removeKeyListener(keyAction);
    }

    public void printScreen(GLAutoDrawable drawable, String dir, String tech, boolean exportAlpha) throws GLException, IOException {
        final String fn = font.getFullFamilyName(null).toString();
        printScreen(drawable, dir, tech, fn.replace(' ', '_'), exportAlpha);
    }

    float fontHeadScale = 1f;

    public class KeyAction implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
            if(userInput) {
                return;
            }
            final short s = e.getKeySymbol();
            if(s == KeyEvent.VK_3) {
                if( e.isAltDown() ) {
                    fontHeadIncr(1);
                } else {
                    fontBottomIncr(1);
                }
            }
            else if(s == KeyEvent.VK_4) {
                if( e.isAltDown() ) {
                    fontHeadIncr(-1);
                } else {
                    fontBottomIncr(-1);
                }
            }
            else if(s == KeyEvent.VK_H) {
                switchHeadBox();
            }
            else if(s == KeyEvent.VK_F) {
                drawFPS = !drawFPS;
            }
            else if(s == KeyEvent.VK_SPACE) {
                nextFontSet();
            }
            else if(s == KeyEvent.VK_I) {
                userInput = true;
                setIgnoreInput(true);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if( !e.isPrintableKey() || e.isAutoRepeat() ) {
                return;
            }
            if(userInput) {
                final short k = e.getKeySymbol();
                if( KeyEvent.VK_ENTER == k ) {
                    userInput = false;
                    setIgnoreInput(false);
                } else if( KeyEvent.VK_BACK_SPACE == k && userString.length()>0) {
                    userString.deleteCharAt(userString.length()-1);
                } else {
                    final char c = e.getKeyChar();
                    if( font.isPrintableChar( c ) ) {
                        userString.append(c);
                    }
                }
            }
        }
    }
}
