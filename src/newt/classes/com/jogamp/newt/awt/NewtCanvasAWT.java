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
 

package com.jogamp.newt.awt;

import java.awt.AWTKeyStroke;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.beans.Beans;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;

import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.OffscreenLayerOption;
import javax.media.nativewindow.WindowClosingProtocol;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.AWTPrintLifecycle;
import javax.swing.MenuSelectionManager;

import jogamp.nativewindow.awt.AWTMisc;
import jogamp.newt.Debug;
import jogamp.newt.WindowImpl;
import jogamp.newt.awt.NewtFactoryAWT;
import jogamp.newt.awt.event.AWTParentWindowAdapter;
import jogamp.newt.driver.DriverClearFocus;
import jogamp.opengl.awt.AWTTilePainter;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.awt.AWTEDTExecutor;
import com.jogamp.nativewindow.awt.AWTWindowClosingProtocol;
import com.jogamp.nativewindow.awt.JAWTWindow;
import com.jogamp.newt.Display;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.awt.AWTAdapter;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTMouseAdapter;
import com.jogamp.opengl.util.GLDrawableUtil;
import com.jogamp.opengl.util.TileRenderer;

/**
 * AWT {@link java.awt.Canvas Canvas} containing a NEWT {@link Window} using native parenting.
 *  
 * <h5><A NAME="java2dgl">Offscreen Layer Remarks</A></h5>
 * 
 * {@link OffscreenLayerOption#setShallUseOffscreenLayer(boolean) setShallUseOffscreenLayer(true)}
 * maybe called to use an offscreen drawable (FBO or PBuffer) allowing 
 * the underlying JAWT mechanism to composite the image, if supported.
 */
@SuppressWarnings("serial")
public class NewtCanvasAWT extends java.awt.Canvas implements WindowClosingProtocol, OffscreenLayerOption, AWTPrintLifecycle {
    public static final boolean DEBUG = Debug.debug("Window");

    private JAWTWindow jawtWindow = null;
    private boolean shallUseOffscreenLayer = false;
    private Window newtChild = null;
    private boolean newtChildAttached = false;
    private boolean isOnscreen = true;
    private WindowClosingMode newtChildCloseOp;
    private AWTParentWindowAdapter awtAdapter = null;
    private AWTAdapter awtMouseAdapter = null;
    private AWTAdapter awtKeyAdapter = null;
    
    private AWTWindowClosingProtocol awtWindowClosingProtocol =
          new AWTWindowClosingProtocol(this, new Runnable() {
                public void run() {
                    NewtCanvasAWT.this.destroyImpl(false /* removeNotify */, true /* windowClosing */);
                }
            }, new Runnable() {
                public void run() {
                    if( newtChild != null ) {
                        newtChild.sendWindowEvent(WindowEvent.EVENT_WINDOW_DESTROY_NOTIFY);
                    }
                }                
            } );

    /**
     * Instantiates a NewtCanvas without a NEWT child.<br>
     */
    public NewtCanvasAWT() {
        super();
    }

    /**
     * Instantiates a NewtCanvas without a NEWT child.<br>
     */
    public NewtCanvasAWT(GraphicsConfiguration gc) {
        super(gc);
    }

    /**
     * Instantiates a NewtCanvas with a NEWT child.
     */
    public NewtCanvasAWT(Window child) {
        super();
        setNEWTChild(child);
    }

    /**
     * Instantiates a NewtCanvas with a NEWT child.
     */
    public NewtCanvasAWT(GraphicsConfiguration gc, Window child) {
        super(gc);
        setNEWTChild(child);
    }
    
    public void setShallUseOffscreenLayer(boolean v) {
        shallUseOffscreenLayer = v;
    }
    
    public final boolean getShallUseOffscreenLayer() {
        return shallUseOffscreenLayer;        
    }
    
    public final boolean isOffscreenLayerSurfaceEnabled() { 
        return jawtWindow.isOffscreenLayerSurfaceEnabled();
    }
      
    /** 
     * Returns true if the AWT component is parented to an {@link java.applet.Applet}, 
     * otherwise false. This information is valid only after {@link #addNotify()} is issued, 
     * ie. before adding the component to the AWT tree and make it visible. 
     */
    public boolean isApplet() {
        return jawtWindow.isApplet();
    }

    boolean isParent() {
        return null!=newtChild && jawtWindow == newtChild.getParent();        
    }
    
    boolean isFullscreen() {
        return null != newtChild && newtChild.isFullscreen();
    }
    
    class FocusAction implements Window.FocusRunnable {
        public boolean run() {
            final boolean isParent = isParent();
            final boolean isFullscreen = isFullscreen();
            if(DEBUG) {
                System.err.println("NewtCanvasAWT.FocusAction: "+Display.getThreadName()+", isOnscreen "+isOnscreen+", hasFocus "+hasFocus()+", isParent "+isParent+", isFS "+isFullscreen);
            }
            if(isParent && !isFullscreen) {
                // Newt-EDT -> AWT-EDT may freeze Window's native peer requestFocus.
                if(!hasFocus()) {
                    // Acquire the AWT focus 1st for proper AWT traversal
                    NewtCanvasAWT.super.requestFocus();
                }
                if(isOnscreen) {
                    // Remove the AWT focus in favor of the native NEWT focus
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                }
            }
            return false; // NEWT shall proceed requesting the native focus
        }
    }
    private FocusAction focusAction = new FocusAction();
    
    WindowListener clearAWTMenusOnNewtFocus = new WindowAdapter() {
          @Override
          public void windowResized(WindowEvent e) {
              updateLayoutSize();
          }
          @Override
          public void windowGainedFocus(WindowEvent arg0) {
              if( isParent() && !isFullscreen() ) {
                  MenuSelectionManager.defaultManager().clearSelectedPath();
              }
          }
    };

    class FocusTraversalKeyListener implements KeyListener {
         public void keyPressed(KeyEvent e) {
             if( isParent() && !isFullscreen() ) {
                 handleKey(e, false);
             }
         }
         public void keyReleased(KeyEvent e) {
             if( isParent() && !isFullscreen() ) {
                 handleKey(e, true);
             }
         }
         
         void handleKey(KeyEvent evt, boolean onRelease) {   
             if(null == keyboardFocusManager) {
                 throw new InternalError("XXX");
             }
             final AWTKeyStroke ks = AWTKeyStroke.getAWTKeyStroke(evt.getKeyCode(), evt.getModifiers(), onRelease);
             boolean suppress = false;
             if(null != ks) {
                 final Set<AWTKeyStroke> fwdKeys = keyboardFocusManager.getDefaultFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS); 
                 final Set<AWTKeyStroke> bwdKeys = keyboardFocusManager.getDefaultFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);
                 if(fwdKeys.contains(ks)) {
                     final Component nextFocus = AWTMisc.getNextFocus(NewtCanvasAWT.this, true /* forward */);
                     if(DEBUG) {
                         System.err.println("NewtCanvasAWT.focusKey (fwd): "+ks+", current focusOwner "+keyboardFocusManager.getFocusOwner()+", hasFocus: "+hasFocus()+", nextFocus "+nextFocus);
                     }
                     // Newt-EDT -> AWT-EDT may freeze Window's native peer requestFocus.
                     nextFocus.requestFocus();
                     suppress = true;
                 } else if(bwdKeys.contains(ks)) {
                     final Component prevFocus = AWTMisc.getNextFocus(NewtCanvasAWT.this, false /* forward */);
                     if(DEBUG) {
                         System.err.println("NewtCanvasAWT.focusKey (bwd): "+ks+", current focusOwner "+keyboardFocusManager.getFocusOwner()+", hasFocus: "+hasFocus()+", prevFocus "+prevFocus);
                     }
                     // Newt-EDT -> AWT-EDT may freeze Window's native peer requestFocus.
                     prevFocus.requestFocus();
                     suppress = true;
                 }
             }
             if(suppress) {
                 evt.setConsumed(true);                 
             }
             if(DEBUG) {
                 System.err.println("NewtCanvasAWT.focusKey: XXX: "+ks);
             }
         }
    }
    private final FocusTraversalKeyListener newtFocusTraversalKeyListener = new FocusTraversalKeyListener(); 

    class FocusPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            final Object oldF = evt.getOldValue();
            final Object newF = evt.getNewValue();
            final boolean isParent = isParent();
            final boolean isFullscreen = isFullscreen(); 
            if(DEBUG) {
                System.err.println("NewtCanvasAWT.FocusProperty: "+evt.getPropertyName()+", src "+evt.getSource()+", "+oldF+" -> "+newF+", isParent "+isParent+", isFS "+isFullscreen);
            }
            if(isParent && !isFullscreen) {
                if(oldF == NewtCanvasAWT.this && newF == null) {
                    // focus traversal to NEWT - NOP
                    if(DEBUG) {                    
                        System.err.println("NewtCanvasAWT.FocusProperty: NEWT focus traversal");
                    }
                } else if(null != newF && newF != NewtCanvasAWT.this) {
                    // focus traversal to another AWT component
                    if(DEBUG) {                    
                        System.err.println("NewtCanvasAWT.FocusProperty: lost focus - clear focus");
                    }
                    if(newtChild.getDelegatedWindow() instanceof DriverClearFocus) {
                        ((DriverClearFocus)newtChild.getDelegatedWindow()).clearFocus();
                    }
                }
            }
        }        
    }
    private final FocusPropertyChangeListener focusPropertyChangeListener = new FocusPropertyChangeListener();
    private volatile KeyboardFocusManager keyboardFocusManager = null;

    /** 
     * Sets a new NEWT child, provoking reparenting.
     * <p>
     * A previously detached <code>newChild</code> will be released to top-level status
     * and made invisible. 
     * </p>
     * <p>
     * Note: When switching NEWT child's, detaching the previous first via <code>setNEWTChild(null)</code> 
     * produced much cleaner visual results. 
     * </p>
     * @return the previous attached newt child.  
     */
    public Window setNEWTChild(Window newChild) {
        final Window prevChild = newtChild;
        if(DEBUG) {
            System.err.println("NewtCanvasAWT.setNEWTChild.0: win "+newtWinHandleToHexString(prevChild)+" -> "+newtWinHandleToHexString(newChild));
        }
        final java.awt.Container cont = AWTMisc.getContainer(this);
        // remove old one
        if(null != newtChild) {
            detachNewtChild( cont );
            newtChild = null;
        }
        // add new one, reparent only if ready
        newtChild = newChild;

        updateLayoutSize();
        // will be done later at paint/display/..: attachNewtChild(cont);
        
        return prevChild;
    }
    
    private final void updateLayoutSize() {
        if( null != newtChild ) {
            // use NEWT child's size for min/pref size!
            java.awt.Dimension minSize = new java.awt.Dimension(newtChild.getWidth(), newtChild.getHeight());
            setMinimumSize(minSize);
            setPreferredSize(minSize);
        }        
    }

    /** @return the current NEWT child */
    public Window getNEWTChild() {
        return newtChild;
    }

    /** @return this AWT Canvas NativeWindow representation, may be null in case {@link #removeNotify()} has been called,
     * or {@link #addNotify()} hasn't been called yet.*/
    public NativeWindow getNativeWindow() { return jawtWindow; }
    
    public WindowClosingMode getDefaultCloseOperation() {
        return awtWindowClosingProtocol.getDefaultCloseOperation();
    }

    public WindowClosingMode setDefaultCloseOperation(WindowClosingMode op) {
        return awtWindowClosingProtocol.setDefaultCloseOperation(op);
    }

    @Override
    public void addNotify() {
        if( Beans.isDesignTime() ) {
            super.addNotify();
        } else {
            // before native peer is valid: X11
            disableBackgroundErase();
    
            // creates the native peer
            super.addNotify();
    
            // after native peer is valid: Windows
            disableBackgroundErase();
                
            jawtWindow = NewtFactoryAWT.getNativeWindow(this, null != newtChild ? newtChild.getRequestedCapabilities() : null);          
            jawtWindow.setShallUseOffscreenLayer(shallUseOffscreenLayer);
            
            if(DEBUG) {
                // if ( isShowing() == false ) -> Container was not visible yet.
                // if ( isShowing() == true  ) -> Container is already visible.
                System.err.println("NewtCanvasAWT.addNotify: win "+newtWinHandleToHexString(newtChild)+
                                   ", comp "+this+", visible "+isVisible()+", showing "+isShowing()+
                                   ", displayable "+isDisplayable()+", cont "+AWTMisc.getContainer(this));
            }
        }
        awtWindowClosingProtocol.addClosingListener();
    }

    @Override
    public void removeNotify() {
        awtWindowClosingProtocol.removeClosingListener();
        
        if( Beans.isDesignTime() ) {
            super.removeNotify();
        } else {
            destroyImpl(true /* removeNotify */, false /* windowClosing */);
            super.removeNotify();
        }
    }

    /**
     * Destroys this resource:
     * <ul>
     *   <li> Make the NEWT Child invisible </li>
     *   <li> Disconnects the NEWT Child from this Canvas NativeWindow, reparent to NULL </li>
     *   <li> Issues <code>destroy()</code> on the NEWT Child</li>
     *   <li> Remove reference to the NEWT Child</li>
     * </ul>
     * @see Window#destroy()
     */
    public final void destroy() {
        destroyImpl(false /* removeNotify */, false /* windowClosing */);
    }
    
    private final void destroyImpl(boolean removeNotify, boolean windowClosing) {
        if( null !=newtChild ) {
            java.awt.Container cont = AWTMisc.getContainer(this);
            if(DEBUG) {
                System.err.println("NewtCanvasAWT.destroy(removeNotify "+removeNotify+", windowClosing "+windowClosing+"): nw "+newtWinHandleToHexString(newtChild)+", from "+cont);
            }
            detachNewtChild(cont);
            
            if( !removeNotify ) {
                final Window cWin = newtChild;
                final Window dWin = cWin.getDelegatedWindow();
                newtChild=null;
                if( windowClosing && dWin instanceof WindowImpl ) {
                    ((WindowImpl)dWin).windowDestroyNotify(true);
                } else {
                    cWin.destroy();
                }
            }            
        }
        if( ( removeNotify || windowClosing ) && null!=jawtWindow ) {
            NewtFactoryAWT.destroyNativeWindow(jawtWindow);
            jawtWindow=null;
        }        
    }
    
    @Override
    public void paint(Graphics g) {
        if( validateComponent(true) && !printActive ) {
            newtChild.windowRepaint(0, 0, getWidth(), getHeight());
        }
    }
    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void reshape(int x, int y, int width, int height) {
        synchronized (getTreeLock()) { // super.reshape(..) claims tree lock, so we do extend it's lock over reshape
            super.reshape(x, y, width, height);
            if(DEBUG) {
                System.err.println("NewtCanvasAWT.reshape: "+x+"/"+y+" "+width+"x"+height);
            }
            if( validateComponent(true) ) {
                // newtChild.setSize(width, height);
            }
        }
    }
     
    private volatile boolean printActive = false;
    private boolean printUseAA = false;
    private GLAnimatorControl printAnimator = null; 
    private GLAutoDrawable printGLAD = null;
    private AWTTilePainter printAWTTiles = null;

    @Override
    public void setupPrint(Graphics2D g2d, double scaleMatX, double scaleMatY) {
        if( !validateComponent(true) ) {
            if(DEBUG) {
                System.err.println(getThreadName()+": Info: NewtCanvasAWT setupPrint - skipped GL render, drawable not valid yet");
            }
            return; // not yet available ..
        }
        if( !isVisible() ) {
            if(DEBUG) {
                System.err.println(getThreadName()+": Info: NewtCanvasAWT setupPrint - skipped GL render, drawable visible");
            }
            return; // not yet available ..
        }
        printActive = true; 
        final RenderingHints rHints = g2d.getRenderingHints();
        {
            final Object _useAA = rHints.get(RenderingHints.KEY_ANTIALIASING);
            printUseAA = null != _useAA && ( _useAA == RenderingHints.VALUE_ANTIALIAS_DEFAULT || _useAA == RenderingHints.VALUE_ANTIALIAS_ON );
        }
        if( DEBUG ) {
            System.err.println("AWT print.setup: canvasSize "+getWidth()+"x"+getWidth()+", scaleMat "+scaleMatX+" x "+scaleMatY+", useAA "+printUseAA+", printAnimator "+printAnimator);
            AWTTilePainter.dumpHintsAndScale(g2d);
        }
        final int componentCount = isOpaque() ? 3 : 4;
        final TileRenderer printRenderer = new TileRenderer();
        printAWTTiles = new AWTTilePainter(printRenderer, componentCount, scaleMatX, scaleMatY, DEBUG);
        AWTEDTExecutor.singleton.invoke(getTreeLock(), true /* allowOnNonEDT */, true /* wait */, setupPrintOnEDT);
    }  
    private final Runnable setupPrintOnEDT = new Runnable() {
        @Override
        public void run() {
            final GLAutoDrawable glad;
            if( null != newtChild && newtChild instanceof GLAutoDrawable ) {
                glad = (GLAutoDrawable)newtChild;
            } else {
                if( DEBUG ) {
                    System.err.println("AWT print.setup exit, newtChild not a GLAutoDrawable: "+newtChild);
                }
                printAWTTiles = null;
                printActive = false;
                return;
            }
            printAnimator =  glad.getAnimator();
            if( null != printAnimator ) {
                printAnimator.remove(glad);
            }
            final GLCapabilities caps = (GLCapabilities)glad.getChosenGLCapabilities().cloneMutable();
            final GLProfile glp = caps.getGLProfile();
            if( caps.getSampleBuffers() ) {
                // bug / issue w/ swapGLContextAndAllGLEventListener and onscreen MSAA w/ NV/GLX
                printGLAD = glad;
            } else {
                caps.setDoubleBuffered(false);
                caps.setOnscreen(false);
                if( printUseAA && !caps.getSampleBuffers() ) {
                    if ( !glp.isGL2ES3() ) {
                        if( DEBUG ) {
                            System.err.println("Ignore MSAA due to gl-profile < GL2ES3");
                        }
                        printUseAA = false;
                    } else {
                        caps.setSampleBuffers(true);
                        caps.setNumSamples(8);
                    }
                }
                final GLDrawableFactory factory = GLDrawableFactory.getFactory(caps.getGLProfile());
                printGLAD = factory.createOffscreenAutoDrawable(null, caps, null, DEFAULT_PRINT_TILE_SIZE, DEFAULT_PRINT_TILE_SIZE, null);
                GLDrawableUtil.swapGLContextAndAllGLEventListener(glad, printGLAD);
            }

            printAWTTiles.renderer.setTileSize(printGLAD.getWidth(), printGLAD.getHeight(), 0);
            printAWTTiles.renderer.attachToAutoDrawable(printGLAD);
            if( DEBUG ) {
                System.err.println("AWT print.setup "+printAWTTiles);
                System.err.println("AWT print.setup AA "+printUseAA+", "+caps);
                System.err.println("AWT print.setup "+printGLAD);
            }
        }
    };

    @Override
    public void releasePrint() {
        if( !printActive || null == printGLAD ) {
            throw new IllegalStateException("setupPrint() not called");
        }
        // sendReshape = false; // clear reshape flag
        AWTEDTExecutor.singleton.invoke(getTreeLock(), true /* allowOnNonEDT */, true /* wait */, releasePrintOnEDT);
        newtChild.sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED); // trigger a resize/relayout to listener
        
    }
    private final Runnable releasePrintOnEDT = new Runnable() {
        @Override
        public void run() {
            if( DEBUG ) {
                System.err.println("AWT print.release "+printAWTTiles);
            }
            final GLAutoDrawable glad = (GLAutoDrawable)newtChild;
            printAWTTiles.dispose();
            printAWTTiles= null;
            if( printGLAD != glad ) {
                GLDrawableUtil.swapGLContextAndAllGLEventListener(printGLAD, glad);
                printGLAD.destroy();
            }
            printGLAD = null;
            if( null != printAnimator ) {
                printAnimator.add(glad);
                printAnimator = null;
            }
            printActive = false;
        }
    };

    @Override
    public void print(Graphics graphics) {
        if( !printActive || null == printGLAD ) {
            throw new IllegalStateException("setupPrint() not called");
        }
        if(DEBUG && !EventQueue.isDispatchThread()) {
            System.err.println(getThreadName()+": Warning: GLCanvas print - not called from AWT-EDT");
            // we cannot dispatch print on AWT-EDT due to printing internal locking ..
        }

        final Graphics2D g2d = (Graphics2D)graphics;
        printAWTTiles.setupGraphics2DAndClipBounds(g2d);
        try {
            final TileRenderer tileRenderer = printAWTTiles.renderer;
            if( DEBUG ) {
                System.err.println("AWT print.0: "+tileRenderer);
            }
            do {
                tileRenderer.display();
            } while ( !tileRenderer.eot() );
        } finally {
            printAWTTiles.resetGraphics2D();
        }
        if( DEBUG ) {
            System.err.println("AWT print.X: "+printAWTTiles);
        }
    }
    
    private final void requestFocusNEWTChild() {
        if(null!=newtChild) {
            newtChild.setFocusAction(null);
            if(isOnscreen) {                    
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
            }
            newtChild.requestFocus();
            newtChild.setFocusAction(focusAction);
        }
    }

    @Override
    public void requestFocus() {
        super.requestFocus();
        requestFocusNEWTChild();
    }

    @Override
    public boolean requestFocus(boolean temporary) {
        final boolean res = super.requestFocus(temporary);
        if(res) {
            requestFocusNEWTChild();
        }
        return res;
    }

    @Override
    public boolean requestFocusInWindow() {
        final boolean res = super.requestFocusInWindow();
        if(res) {
            requestFocusNEWTChild();
        }
        return res;
    }

    @Override
    public boolean requestFocusInWindow(boolean temporary) {
        final boolean res = super.requestFocusInWindow(temporary);
        if(res) {
            requestFocusNEWTChild();
        }
        return res;
    }

    private final boolean validateComponent(boolean attachNewtChild) {
        if( Beans.isDesignTime() || !isDisplayable() ) {
            return false;
        }
        if ( null == newtChild || null == jawtWindow ) {
            return false;
        }
        if( 0 >= getWidth() || 0 >= getHeight() ) {
            return false;
        }
        
        if( attachNewtChild && !newtChildAttached && null != newtChild ) {
            attachNewtChild();
        }

        return true;
    }
    
    private final void configureNewtChild(boolean attach) {
        if(null!=awtAdapter) {
          awtAdapter.removeFrom(this);
          awtAdapter=null;
        }
        if(null!=awtMouseAdapter) {
            awtMouseAdapter.removeFrom(this);
            awtMouseAdapter = null;
        }
        if(null!=awtKeyAdapter) {
            awtKeyAdapter.removeFrom(this);
            awtKeyAdapter = null;
        }
        if(null != keyboardFocusManager) {
            keyboardFocusManager.removePropertyChangeListener("focusOwner", focusPropertyChangeListener);
            keyboardFocusManager = null;
        }
        
        if( null != newtChild ) {
            newtChild.setKeyboardFocusHandler(null);
            if(attach) {
                if(null == jawtWindow.getGraphicsConfiguration()) {
                    throw new InternalError("XXX");
                }                
                isOnscreen = jawtWindow.getGraphicsConfiguration().getChosenCapabilities().isOnscreen();
                awtAdapter = (AWTParentWindowAdapter) new AWTParentWindowAdapter(jawtWindow, newtChild).addTo(this);
                awtAdapter.removeWindowClosingFrom(this); // we utilize AWTWindowClosingProtocol triggered destruction!
                newtChild.addWindowListener(clearAWTMenusOnNewtFocus);
                newtChild.setFocusAction(focusAction); // enable AWT focus traversal
                newtChildCloseOp = newtChild.setDefaultCloseOperation(WindowClosingMode.DO_NOTHING_ON_CLOSE);
                keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                keyboardFocusManager.addPropertyChangeListener("focusOwner", focusPropertyChangeListener);
                // force this AWT Canvas to be focus-able,
                // since this it is completely covered by the newtChild (z-order).
                setFocusable(true);
                if(isOnscreen) {
                    // onscreen newt child needs to fwd AWT focus
                    newtChild.setKeyboardFocusHandler(newtFocusTraversalKeyListener);
                } else {
                    // offscreen newt child requires AWT to fwd AWT key/mouse event
                    awtMouseAdapter = new AWTMouseAdapter(newtChild).addTo(this);
                    awtKeyAdapter = new AWTKeyAdapter(newtChild).addTo(this);
                }
            } else {
                newtChild.removeWindowListener(clearAWTMenusOnNewtFocus);
                newtChild.setFocusAction(null);
                newtChild.setDefaultCloseOperation(newtChildCloseOp);
                setFocusable(false);
            }
        }
    }

    private final void attachNewtChild() {
      if( null == newtChild || null == jawtWindow || newtChildAttached ) {
          return; // nop
      }
      if(DEBUG) {
          // if ( isShowing() == false ) -> Container was not visible yet.
          // if ( isShowing() == true  ) -> Container is already visible.
          System.err.println("NewtCanvasAWT.attachNewtChild.0 @ "+Thread.currentThread().getName()+": win "+newtWinHandleToHexString(newtChild)+
                             ", EDTUtil: cur "+newtChild.getScreen().getDisplay().getEDTUtil()+
                             ", comp "+this+", visible "+isVisible()+", showing "+isShowing()+", displayable "+isDisplayable()+
                             ", cont "+AWTMisc.getContainer(this));
      }

      newtChildAttached = true;
      newtChild.setFocusAction(null); // no AWT focus traversal ..
      if(DEBUG) {
        System.err.println("NewtCanvasAWT.attachNewtChild.1: newtChild: "+newtChild);
      }
      final int w = getWidth();
      final int h = getHeight();
      if(DEBUG) {
          System.err.println("NewtCanvasAWT.attachNewtChild.2: size "+w+"x"+h);
      }
      newtChild.setVisible(false);
      newtChild.setSize(w, h);
      newtChild.reparentWindow(jawtWindow);
      newtChild.addSurfaceUpdatedListener(jawtWindow);
      if( Platform.OSType.MACOS == Platform.getOSType() && jawtWindow.isOffscreenLayerSurfaceEnabled() ) {
          AWTEDTExecutor.singleton.invoke(false, forceRelayout);
      }
      newtChild.setVisible(true);
      configureNewtChild(true);
      newtChild.sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED); // trigger a resize/relayout to listener
      
      if(DEBUG) {
          System.err.println("NewtCanvasAWT.attachNewtChild.X: win "+newtWinHandleToHexString(newtChild)+", EDTUtil: cur "+newtChild.getScreen().getDisplay().getEDTUtil()+", comp "+this);
      }
    }
    private final Runnable forceRelayout = new Runnable() {
        public void run() {
            if(DEBUG) {
                System.err.println("NewtCanvasAWT.forceRelayout.0");
            }
            // Hack to force proper native AWT layout incl. CALayer components on OSX
            final java.awt.Component component = NewtCanvasAWT.this;
            final int cW = component.getWidth();
            final int cH = component.getHeight();
            component.setSize(cW+1, cH+1);
            component.setSize(cW, cH);
            if(DEBUG) {
                System.err.println("NewtCanvasAWT.forceRelayout.X");
            }
        }  };
    
    private final void detachNewtChild(java.awt.Container cont) {
      if( null == newtChild || null == jawtWindow || !newtChildAttached ) {
          return; // nop
      }
      if(DEBUG) {
          // if ( isShowing() == false ) -> Container was not visible yet.
          // if ( isShowing() == true  ) -> Container is already visible.
          System.err.println("NewtCanvasAWT.detachNewtChild.0: win "+newtWinHandleToHexString(newtChild)+
                             ", EDTUtil: cur "+newtChild.getScreen().getDisplay().getEDTUtil()+
                             ", comp "+this+", visible "+isVisible()+", showing "+isShowing()+", displayable "+isDisplayable()+
                             ", cont "+cont);
      }

      newtChild.removeSurfaceUpdatedListener(jawtWindow);      
      newtChildAttached = false;
      newtChild.setFocusAction(null); // no AWT focus traversal ..
      configureNewtChild(false);
      newtChild.setVisible(false);
      
      newtChild.reparentWindow(null); // will destroy context (offscreen -> onscreen) and implicit detachSurfaceLayer
      
      if(DEBUG) {
          System.err.println("NewtCanvasAWT.detachNewtChild.X: win "+newtWinHandleToHexString(newtChild)+", EDTUtil: cur "+newtChild.getScreen().getDisplay().getEDTUtil()+", comp "+this);
      }
    }

  // Disables the AWT's erasing of this Canvas's background on Windows
  // in Java SE 6. This internal API is not available in previous
  // releases, but the system property
  // -Dsun.awt.noerasebackground=true can be specified to get similar
  // results globally in previous releases.
  private static boolean disableBackgroundEraseInitialized;
  private static Method  disableBackgroundEraseMethod;
  private void disableBackgroundErase() {
    if (!disableBackgroundEraseInitialized) {
      try {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
              try {
                Class<?> clazz = getToolkit().getClass();
                while (clazz != null && disableBackgroundEraseMethod == null) {
                  try {
                    disableBackgroundEraseMethod =
                      clazz.getDeclaredMethod("disableBackgroundErase",
                                              new Class[] { Canvas.class });
                    disableBackgroundEraseMethod.setAccessible(true);
                  } catch (Exception e) {
                    clazz = clazz.getSuperclass();
                  }
                }
              } catch (Exception e) {
              }
              return null;
            }
          });
      } catch (Exception e) {
      }
      disableBackgroundEraseInitialized = true;
      if(DEBUG) {
        System.err.println("NewtCanvasAWT: TK disableBackgroundErase method found: "+
                (null!=disableBackgroundEraseMethod));
      }
    }
    if (disableBackgroundEraseMethod != null) {
      Throwable t=null;
      try {
        disableBackgroundEraseMethod.invoke(getToolkit(), new Object[] { this });
      } catch (Exception e) {
        t = e;
      }
      if(DEBUG) {
        System.err.println("NewtCanvasAWT: TK disableBackgroundErase error: "+t);
      }
    }
  }

  protected static String getThreadName() { return Thread.currentThread().getName(); }
  
  static String newtWinHandleToHexString(Window w) {
      return null != w ? toHexString(w.getWindowHandle()) : "nil";
  }
  static String toHexString(long l) {
      return "0x"+Long.toHexString(l);
  }
}

