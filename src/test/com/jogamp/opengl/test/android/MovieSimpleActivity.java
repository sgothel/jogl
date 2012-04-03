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

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import jogamp.newt.driver.android.AndroidWindow;
import jogamp.newt.driver.android.NewtBaseActivity;

import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.opengl.av.GLMediaPlayer;
import com.jogamp.opengl.av.GLMediaPlayerFactory;
import com.jogamp.opengl.test.junit.jogl.demos.es2.av.MovieSimple;
import com.jogamp.opengl.util.Animator;

import android.os.Bundle;
import android.util.Log;

public class MovieSimpleActivity extends NewtBaseActivity {
   static String TAG = "NEWTGearsES2Activity";
   
   MouseAdapter demoMouseListener = new MouseAdapter() {
       public void mouseClicked(MouseEvent e) {
           Object src = e.getSource();
           if(src instanceof AndroidWindow) {
               ((AndroidWindow)src).getAndroidView().bringToFront();
           }
       } };
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
       Log.d(TAG, "onCreate - 0");
       super.onCreate(savedInstanceState);
       
       String[] urls = new String[] {                    
               System.getProperty("jnlp.media0_url2"),
               System.getProperty("jnlp.media0_url1"),
               System.getProperty("jnlp.media0_url0") };
       final URLConnection ucH = getResource(urls, 0);
       if(null == ucH) { throw new RuntimeException("no media reachable: "+Arrays.asList(urls)); }
       URLConnection ucL = null; // getResource(urls, 1);
       if(null == ucL) { ucL = ucH; }
       
       setTransparencyTheme();
       setFullscreenFeature(getWindow(), true);
           
       android.view.ViewGroup viewGroup = new android.widget.FrameLayout(getActivity().getApplicationContext());
       getWindow().setContentView(viewGroup);
       
       // also initializes JOGL
       final GLCapabilities capsMain = new GLCapabilities(GLProfile.getGL2ES2());
       capsMain.setBackgroundOpaque(false);
       final GLCapabilities capsHUD = new GLCapabilities(GLProfile.getGL2ES2());       

       // screen for layout params ..
       final com.jogamp.newt.Display dpy = NewtFactory.createDisplay(null);
       final com.jogamp.newt.Screen scrn = NewtFactory.createScreen(dpy, 0);
       scrn.addReference();
              
       try {
           Animator animator = new Animator();
           setAnimator(animator);
           
           // Main
           final MovieSimple demoMain = new MovieSimple(ucH.getURL());
           demoMain.setEffects(MovieSimple.EFFECT_GRADIENT_BOTTOM2TOP);
           demoMain.setTransparency(0.9f);           
           final GLWindow glWindowMain = GLWindow.create(scrn, capsMain);
           glWindowMain.addMouseListener(demoMouseListener);
           glWindowMain.setFullscreen(true);
           // setContentView(getWindow(), glWindowMain);
           viewGroup.addView(((AndroidWindow)glWindowMain.getDelegatedWindow()).getAndroidView(), 0);
           registerNEWTWindow(glWindowMain);
           glWindowMain.addGLEventListener(demoMain);
           animator.add(glWindowMain);
           glWindowMain.setVisible(true);

           final GLMediaPlayer demoMP = GLMediaPlayerFactory.create();
           demoMP.initStream(ucL.getURL());
           final MovieSimple demoHUD = new MovieSimple(demoMP);
           final GLWindow glWindowHUD = GLWindow.create(scrn, capsHUD);
           glWindowHUD.addMouseListener(demoMouseListener);
           {
               int x2 = scrn.getX();
               int y2 = scrn.getY();
               int w2 = scrn.getWidth()/2;
               int h2 = scrn.getHeight()/2;
               if(0 < demoMP.getWidth() && demoMP.getWidth()<w2) {
                   w2 = demoMP.getWidth();
               }
               if(0 < demoMP.getHeight() && demoMP.getHeight()<h2) {
                   h2 = demoMP.getHeight();
               }
               glWindowHUD.setPosition(x2, y2);
               glWindowHUD.setSize(w2, h2);
               System.err.println("HUD: "+demoMP);
               System.err.println("HUD: "+w2+"x"+h2);
           }
           // addContentView(getWindow(), glWindowHUD, new android.view.ViewGroup.LayoutParams(glWindowHUD.getWidth(), glWindowHUD.getHeight()));
           viewGroup.addView(((AndroidWindow)glWindowHUD.getDelegatedWindow()).getAndroidView(), 1, new android.view.ViewGroup.LayoutParams(glWindowHUD.getWidth(), glWindowHUD.getHeight()));
           registerNEWTWindow(glWindowHUD);                  
           glWindowHUD.addGLEventListener(demoHUD);
           animator.add(glWindowHUD);
           glWindowHUD.setVisible(true);
           
           animator.setUpdateFPSFrames(60, System.err);
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
           if(null != path[i]) {
               uc = IOUtil.getResource(path[i], null);
               Log.d(TAG, "Stream: <"+path[i]+">: "+(null!=uc));
           }
       }
       return uc;       
   }
}
