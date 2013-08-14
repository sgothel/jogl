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
package com.jogamp.opengl.test.android;

import java.io.IOException;
import java.net.URLConnection;
import java.util.Arrays;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLRunnable;

import jogamp.newt.driver.android.NewtBaseActivity;

import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.opengl.test.junit.jogl.demos.es2.av.MovieSimple;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.av.GLMediaPlayer;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;

public class MovieSimpleActivity1 extends NewtBaseActivity {
   static String TAG = "MovieSimpleActivity1";
   
   MouseAdapter toFrontMouseListener = new MouseAdapter() {
       public void mouseClicked(MouseEvent e) {
           Object src = e.getSource();
           if(src instanceof Window) {
               ((Window)src).requestFocus(false);
           }
       } };
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       
       final boolean mPlayerLocal = Boolean.valueOf(System.getProperty("jnlp.mplayer.local"));
       final boolean mPlayerNoZoom = Boolean.valueOf(System.getProperty("jnlp.mplayer.nozoom"));
       final boolean mPlayerHUD = Boolean.valueOf(System.getProperty("jnlp.mplayer.hud"));
       final boolean mPlayerSharedHUD = mPlayerHUD && Boolean.valueOf(System.getProperty("jnlp.mplayer.hud.shared"));
       Log.d(TAG, "onCreate - 0 - mPlayerLocal "+mPlayerLocal+", mPlayerNoScale "+mPlayerNoZoom+", mPlayerHUD "+mPlayerHUD+", mPlayerSharedHUD "+mPlayerSharedHUD);
       
       String[] urls0 = new String[] {                    
               System.getProperty("jnlp.media0_url2"),
               System.getProperty("jnlp.media0_url1"),
               System.getProperty("jnlp.media0_url0") };       
       final URLConnection urlConnection0 = getResource(urls0, mPlayerLocal ? 2 : 0);
       if(null == urlConnection0) { throw new RuntimeException("no media reachable: "+Arrays.asList(urls0)); }
       
       final URLConnection urlConnection1;
       {
           URLConnection _urlConnection1 = null;
           if(mPlayerHUD && !mPlayerSharedHUD) {
               String[] urls1 = new String[] { System.getProperty("jnlp.media1_url0") };
               _urlConnection1 = getResource(urls1, 0);
           }
           if(null == _urlConnection1) { _urlConnection1 = urlConnection0; }
           urlConnection1 = _urlConnection1;
       }
       
       setTransparencyTheme();
       setFullscreenFeature(getWindow(), true);
           
       final android.view.ViewGroup viewGroup = new android.widget.FrameLayout(getActivity().getApplicationContext());
       getWindow().setContentView(viewGroup);
       
       // also initializes JOGL
       final GLCapabilities capsMain = new GLCapabilities(GLProfile.getGL2ES2());
       capsMain.setBackgroundOpaque(!mPlayerHUD);

       // screen for layout params ..
       final com.jogamp.newt.Display dpy = NewtFactory.createDisplay(null);
       final com.jogamp.newt.Screen scrn = NewtFactory.createScreen(dpy, 0);
       scrn.addReference();
              
       try {
           final Animator animator = new Animator();
           
           // Main           
           final MovieSimple demoMain = new MovieSimple(urlConnection0, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.STREAM_ID_AUTO);
           if(mPlayerHUD) {
               demoMain.setEffects(MovieSimple.EFFECT_GRADIENT_BOTTOM2TOP);
               demoMain.setTransparency(0.9f);
           }
           demoMain.setScaleOrig(mPlayerNoZoom);
           final GLWindow glWindowMain = GLWindow.create(scrn, capsMain);
           {
               final int padding = mPlayerHUD ? 32 : 0;
               final android.view.View androidView = ((jogamp.newt.driver.android.WindowDriver)glWindowMain.getDelegatedWindow()).getAndroidView();
               glWindowMain.setSize(scrn.getWidth()-padding, scrn.getHeight()-padding);
               glWindowMain.setUndecorated(true);
               // setContentView(getWindow(), glWindowMain);
               viewGroup.addView(androidView, new android.widget.FrameLayout.LayoutParams(glWindowMain.getWidth(), glWindowMain.getHeight(), Gravity.BOTTOM|Gravity.RIGHT));
               registerNEWTWindow(glWindowMain);
           }
           
           glWindowMain.addGLEventListener(demoMain);
           animator.add(glWindowMain);
           glWindowMain.setVisible(true);
           
           if(mPlayerHUD) {
                final GLMediaPlayer sharedPlayer = mPlayerSharedHUD ? demoMain.getGLMediaPlayer() : null; 
                final GLCapabilities capsHUD = new GLCapabilities(GLProfile.getGL2ES2());
                capsHUD.setBackgroundOpaque(false);
                final GLWindow glWindowHUD = GLWindow.create(scrn, capsHUD);
                glWindowMain.invoke(false, new GLRunnable() {
                    @Override
                    public boolean run(GLAutoDrawable drawable) {
                        int x2 = scrn.getX();
                        int y2 = scrn.getY();
                        int w2 = scrn.getWidth()/3;
                        int h2 = scrn.getHeight()/3;
                        if(null != sharedPlayer) {
                           if(0 < sharedPlayer.getWidth() && sharedPlayer.getWidth()<scrn.getWidth()/2 &&
                              0 < sharedPlayer.getHeight() && sharedPlayer.getHeight()<scrn.getHeight()/2) {
                               w2 = sharedPlayer.getWidth();
                               h2 = sharedPlayer.getHeight();
                           }
                           glWindowHUD.setSharedContext(glWindowMain.getContext());
                           glWindowHUD.addGLEventListener(new MovieSimple(sharedPlayer));                            
                        } else {
                           try {
                               glWindowHUD.addGLEventListener(new MovieSimple(urlConnection1, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.STREAM_ID_AUTO));
                           } catch (IOException e) {
                               e.printStackTrace();
                           }
                        }
                        glWindowHUD.setPosition(x2, y2);
                        glWindowHUD.setSize(w2, h2);
                        System.err.println("HUD: "+mPlayerHUD);
                        System.err.println("HUD: "+w2+"x"+h2);                            
                        glWindowHUD.addMouseListener(toFrontMouseListener);               

                        viewGroup.post(new Runnable() {
                            public void run() {
                                final android.view.View androidView = ((jogamp.newt.driver.android.WindowDriver)glWindowHUD.getDelegatedWindow()).getAndroidView();
                                // addContentView(getWindow(), glWindowHUD, new android.view.ViewGroup.LayoutParams(glWindowHUD.getWidth(), glWindowHUD.getHeight()));
                                viewGroup.addView(androidView, new android.widget.FrameLayout.LayoutParams(glWindowHUD.getWidth(), glWindowHUD.getHeight(), Gravity.TOP|Gravity.LEFT));
                                registerNEWTWindow(glWindowHUD);  
                                animator.add(glWindowHUD);
                                glWindowHUD.setVisible(true);
                            } } );
                        return true;
                    } } );
           }
           
           animator.setUpdateFPSFrames(60, System.err);
           // animator.setUpdateFPSFrames(-1, null);
           animator.resetFPSCounter();
       } catch (IOException e) {
           e.printStackTrace();
       }
       
       scrn.removeReference();

       Log.d(TAG, "onCreate - X");
   }
   
   static URLConnection getResource(String path[], int off) {
       URLConnection uc = null;
       for(int i=off; null==uc && i<path.length; i++) {
           if(null != path[i] && path[i].length()>0) {
               uc = IOUtil.getResource(path[i], null);
               Log.d(TAG, "Stream: <"+path[i]+">: "+(null!=uc));
           }
       }
       return uc;       
   }
}
