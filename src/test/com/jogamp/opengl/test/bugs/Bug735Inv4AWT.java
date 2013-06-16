package com.jogamp.opengl.test.bugs;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.SwingUtilities;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.LandscapeES2;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;

/**
 * Difference to orig. Bug735Inv0AppletAWT:
 * <pre>
 *   - Use GLEventListener
 *   - Add GLEventListener to GLAutoDrawable
 *   - Use GLAutoDrawable.display() instead of GLAutoDrawable.invoke(true, GLRunnable { init / render })
 *   - Removed MANUAL_FRAME_HANDLING, obsolete due to GLAutoDrawable/GLEventListener 
 *   - Use Animator
 *   - Remove component sizes, use frame based size via validate
 *   - Run frame validation/visibility on AWT-EDT
 *   - Add Wait-For-Key after init (perf-test)
 *   - Remove intermediate applet!
 * </pre>
 * OSX Results:
 * <pre>
 *   - Visible content
 *   - Java6: Fluent animation
 *   - Java7: Stuttering, non-fluent and slow animation
 * </pre>
 */
public class Bug735Inv4AWT {
  static public int AWT  = 0;
  static public int NEWT = 1;
  
  static public int APPLET_WIDTH  = 500;
  static public int APPLET_HEIGHT = 290;
  static public int TOOLKIT       = NEWT;
  static public boolean IGNORE_AWT_REPAINT = false;
  static public boolean USE_ECT = false;
  static public int SWAP_INTERVAL = 0;
  
  //////////////////////////////////////////////////////////////////////////////
  
  static boolean waitForKey = true;  
  static private Frame frame;
  static private Bug735Inv4AWT applet;
  private GLCanvas awtCanvas;
  private GLWindow newtWindow;
  private GLAutoDrawable glad;
  private NewtCanvasAWT newtCanvas;
  private GLEventListener demo;
  private AnimatorBase animator;
  
  private int width;
  private int height;
  
  public void init() {
    width = APPLET_WIDTH;
    height = APPLET_HEIGHT;
    initGL();
  }
  
  public void start() {
    initDraw();
    animator.start();
  }
  
  private void initGL() {
    GLProfile profile = GLProfile.getDefault();
    GLCapabilities caps = new GLCapabilities(profile);
    caps.setBackgroundOpaque(true);
    caps.setOnscreen(true);
    caps.setSampleBuffers(false);
    
    if (TOOLKIT == AWT) {
      awtCanvas = new GLCanvas(caps);
      awtCanvas.setBackground(new Color(0xFFCCCCCC, true));
      awtCanvas.setFocusable(true); 
      
      if (IGNORE_AWT_REPAINT) {
          awtCanvas.setIgnoreRepaint(true);
      }
      glad = awtCanvas;
    } else if (TOOLKIT == NEWT) {      
      newtWindow = GLWindow.create(caps);
      newtCanvas = new NewtCanvasAWT(newtWindow);
      newtCanvas.setBackground(new Color(0xFFCCCCCC, true));
      newtCanvas.setFocusable(true);

      if (IGNORE_AWT_REPAINT) {
        newtCanvas.setIgnoreRepaint(true);
      }
      glad = newtWindow;
    }
    
    demo = new LandscapeES2(SWAP_INTERVAL);
    // demo = new GearsES2(SWAP_INTERVAL);
    glad.addGLEventListener(demo);
    animator = new Animator(glad);
    animator.setExclusiveContext(USE_ECT);
  }
  
  private void initDraw() {
    if (TOOLKIT == AWT) {
      if (awtCanvas.getDelegatedDrawable().isRealized()) {
        // Request the focus here as it cannot work when the window is not visible
        awtCanvas.requestFocus();
      }      
    } else if (TOOLKIT == NEWT) {
      // newtCanvas.repaint();
      // Force the realization
      // newtWindow.display();
      if (newtWindow.isRealized()) {
        // Request the focus here as it cannot work when the window is not visible
        newtCanvas.requestFocus();
      }
    }    
  }
  
  static public void main(String[] args) {    
    if(waitForKey) {
        UITestCase.waitForKey("Start");
    }
    final GraphicsEnvironment environment = 
        GraphicsEnvironment.getLocalGraphicsEnvironment();
    final GraphicsDevice displayDevice = environment.getDefaultScreenDevice();

    frame = new Frame(displayDevice.getDefaultConfiguration());
    // JAU frame.setBackground(new Color(0xCC, 0xCC, 0xCC));
    frame.setTitle("TestBug735Inv4AWT");
    
    // This allows to close the frame.
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });
        
    applet = new Bug735Inv4AWT();
    applet.init();
    
    if (TOOLKIT == AWT) {
        frame.add(applet.awtCanvas);
    } else if (TOOLKIT == NEWT) {
        frame.add(applet.newtCanvas);
    }
    // frame.pack();
    // frame.setResizable(false);
    
    Insets insets = frame.getInsets();
    final int windowW = applet.width + insets.left + insets.right;
    final int windowH = applet.height + insets.top + insets.bottom;
    
    try {
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame.setSize(windowW, windowH);
               frame.validate();
               // frame.pack();
                Rectangle screenRect = displayDevice.getDefaultConfiguration().getBounds();    
                frame.setLocation(screenRect.x + (screenRect.width - applet.width) / 2,
                    screenRect.y + (screenRect.height - applet.height) / 2);    
                
               frame.setVisible(true);               
           }
        });
    } catch (Exception e) {
        e.printStackTrace();
    }        
    
    applet.start();    
  }
}
