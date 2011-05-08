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

import javax.media.opengl.FPSCounter;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLException;

import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.TextRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;

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
    int fontSet = FontFactory.UBUNTU;
    Font font;
    
    int headType = 0;
    boolean drawFPS = false;
    boolean updateFont = true;
    final int fontSizeFixed = 6;
    int fontSize = 40;
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
        "-------Press H to change text---------\n"; 
    
    StringBuffer userString = new StringBuffer();
    boolean userInput = false;
    
    public GPUTextRendererListenerBase01(RenderState rs, int modes, boolean debug, boolean trace) {
        super(TextRenderer.create(rs, modes), modes, debug, trace);        
        this.font = FontFactory.get(fontSet).getDefault();
        dumpFontNames();
        
        this.fontName = font.toString();
        this.fontNameBox = font.getStringBounds(fontName, fontSizeFixed*2);
        switchHeadBox();        
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
            headbox = font.getStringBounds(headtext, fontSizeFixed*3);
        }
    }

    public void display(GLAutoDrawable drawable) {
        final int width = drawable.getWidth();
        final int height = drawable.getHeight();
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f); // Demo02 needs to have this set here as well .. hmm ?
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final TextRenderer textRenderer = (TextRenderer) getRenderer();
        textRenderer.reshapeOrtho(null, width, height, 0.1f, 7000.0f);
        textRenderer.setColorStatic(gl, 0.0f, 0.0f, 0.0f);
        final GLAnimatorControl animator = drawable.getAnimator();
        final boolean _drawFPS = drawFPS && null != animator && animator.getTotalFPSFrames()>10;
        
        if(_drawFPS) {
            final float fps = animator.getTotalFPS();
            final String fpsS = String.valueOf(fps);
            final int fpsSp = fpsS.indexOf('.');
            textRenderer.resetModelview(null);
            textRenderer.translate(gl, fontSizeFixed, fontSizeFixed, -6000);
            textRenderer.drawString3D(gl, font, fpsS.substring(0, fpsSp+2)+" fps", getPosition(), fontSizeFixed*3, getTexSize());
        }
        
        int dx = width-(int)fontNameBox.getWidth()-2 ;
        int dy = height - 10;        
        
        textRenderer.resetModelview(null);
        textRenderer.translate(gl, dx, dy, -6000);
        textRenderer.drawString3D(gl, font, fontName, getPosition(), fontSizeFixed*2, getTexSize());
        
        dx  =  10;
        dy += -(int)fontNameBox.getHeight() - 10;
        
        if(null != headtext) { 
            textRenderer.resetModelview(null);
            textRenderer.translate(gl, dx, dy, -6000);
            textRenderer.drawString3D(gl, font, headtext, getPosition(), fontSizeFixed*3, getTexSize());
        }
        
        textRenderer.reshapePerspective(null, 45.0f, width, height, 0.1f, 7000.0f);             

        textRenderer.resetModelview(null);            
        textRenderer.translate(null, getXTran(), getYTran(), getZoom());
        textRenderer.rotate(gl, getAngle(), 0, 1, 0);
        textRenderer.setColorStatic(gl, 1.0f, 0.0f, 0.0f);
        if(!userInput) {
            textRenderer.drawString3D(gl, font, text2, getPosition(), fontSize, getTexSize());
        } else {
            textRenderer.drawString3D(gl, font, userString.toString(), getPosition(), fontSize, getTexSize());
        }
    }        
        
    public void fontIncr(int v) {
        fontSize = Math.abs((fontSize + v) % fontSizeModulo) ;
        updateFont = true;
        dumpMatrix(true);
    }

    public void nextFontSet() {
        fontSet = ( fontSet == FontFactory.UBUNTU ) ? FontFactory.JAVA : FontFactory.UBUNTU ;
        font = FontFactory.get(fontSet).getDefault();   
        this.fontName = font.getFullFamilyName(null).toString();
        this.fontNameBox = font.getStringBounds(fontName, fontSizeFixed*3);
        dumpFontNames();
    }
    
    public void setFontSet(int set, int family, int stylebits) {
        fontSet = set;
        font = FontFactory.get(fontSet).get(family, stylebits);       
        dumpFontNames();
    }
    
    public boolean isUserInputMode() { return userInput; }
    
    void dumpMatrix(boolean bbox) {
        System.err.println("Matrix: " + getXTran() + "/" + getYTran() + " x"+getZoom() + " @"+getAngle() +" fontSize "+fontSize);
        if(bbox) {
            System.err.println("bbox: "+font.getStringBounds(text2, fontSize));
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
    public void detachFrom(GLWindow window) {
        super.detachFrom(window);
        if ( null == keyAction ) {
            return;
        }
        window.removeKeyListener(keyAction);
    }
    
    public void printScreen(GLAutoDrawable drawable, String dir, String tech, boolean exportAlpha) throws GLException, IOException {
        final String fn = font.getFullFamilyName(null).toString();        
        printScreen(drawable, dir, tech, fn.replace(' ', '_'), exportAlpha);
    }
    
    public class KeyAction implements KeyListener {
        public void keyPressed(KeyEvent arg0) {
            if(userInput) {
                return;
            }
            
            if(arg0.getKeyCode() == KeyEvent.VK_3) {
                fontIncr(10);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_4) {
                fontIncr(-10);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_H) {
                switchHeadBox();
            }  
            else if(arg0.getKeyCode() == KeyEvent.VK_F) {
                drawFPS = !drawFPS; 
            }  
            else if(arg0.getKeyCode() == KeyEvent.VK_SPACE) {      
                nextFontSet();
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_I) {
                userInput = true;
                setIgnoreInput(true);
            }
        }
        
        public void keyTyped(KeyEvent arg0) {
            if(userInput) {                
                char c = arg0.getKeyChar();
                
                if(c == 0x0d) {
                    userInput = false;
                    setIgnoreInput(false);
                } else if(c == 0x08 && userString.length()>0) {
                    userString.deleteCharAt(userString.length()-1);
                } else if( font.isPrintableChar( c ) ) { 
                    userString.append(c);
                }
            }
        }
        public void keyReleased(KeyEvent arg0) {}
    }
}
