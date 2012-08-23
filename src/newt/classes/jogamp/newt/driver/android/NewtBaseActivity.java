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
package jogamp.newt.driver.android;

import java.util.ArrayList;
import java.util.List;

import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.opengl.FPSCounter;

import com.jogamp.newt.Window;
import com.jogamp.opengl.util.Animator;

import jogamp.newt.driver.android.WindowDriver;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

public class NewtBaseActivity extends Activity {
   List<Window> newtWindows = new ArrayList<Window>();
   Animator animator = null;
    
   boolean isDelegatedActivity;
   Activity rootActivity;
   boolean setThemeCalled = false;
      
   public NewtBaseActivity() {
       super();
       isDelegatedActivity = false;
       rootActivity = this;
   }
   
   public void setRootActivity(Activity rootActivity) {
       this.isDelegatedActivity = true;
       this.rootActivity = rootActivity;
   }
   
   public final boolean isDelegatedActivity() {
       return isDelegatedActivity;
   }
   
   public final Activity getActivity() {
       return rootActivity;
   }     
   
   /**
    * This is one of the three registration methods (see below).
    * <p>
    * This methods issues {@link android.view.Window#setContentView(android.view.View, android.view.ViewGroup.LayoutParams) androidWindow.setContenView(newtWindow.getAndroidView())}
    * and finally calls {@link #registerNEWTWindow(Window)}.
    * </p>  
    * @param androidWindow
    * @param newtWindow
    * @see #addContentView(android.view.Window, Window, android.view.ViewGroup.LayoutParams)
    */
   public void setContentView(android.view.Window androidWindow, Window newtWindow) {
       newtWindow = newtWindow.getDelegatedWindow();
       if(newtWindow instanceof WindowDriver) {
           adaptTheme4Transparency(newtWindow.getRequestedCapabilities());
           layoutForNEWTWindow(androidWindow, newtWindow);
           WindowDriver newtAWindow = (WindowDriver)newtWindow;
           androidWindow.setContentView(newtAWindow.getAndroidView());
           registerNEWTWindow(newtAWindow);
       } else {
           throw new IllegalArgumentException("Given NEWT Window is not an Android Window: "+newtWindow.getClass()); 
       }
   }
   /**
    * This is one of the three registration methods (see below).
    * <p>
    * This methods issues {@link android.view.Window#addContentView(android.view.View, android.view.ViewGroup.LayoutParams) androidWindow.addContenView(newtWindow.getAndroidView(), params)}
    * and finally calls {@link #registerNEWTWindow(Window)}.
    * </p>  
    * @param androidWindow
    * @param newtWindow
    * @param params
    * @see #setContentView(android.view.Window, Window)
    * @see #registerNEWTWindow(Window)
    */
   public void addContentView(android.view.Window androidWindow, Window newtWindow, android.view.ViewGroup.LayoutParams params) {
       newtWindow = newtWindow.getDelegatedWindow();
       if(newtWindow instanceof WindowDriver) {
           WindowDriver newtAWindow = (WindowDriver)newtWindow;
           androidWindow.addContentView(newtAWindow.getAndroidView(), params);
           registerNEWTWindow(newtAWindow);
       } else {
           throw new IllegalArgumentException("Given NEWT Window is not an Android Window: "+newtWindow.getClass()); 
       }       
   }
   /**
    * This is one of the three registration methods (see below).
    * <p>
    * This methods simply registers the given NEWT window to ensure it's destruction at {@link #onDestroy()}.
    * </p>  
    * 
    * @param newtWindow
    * @see #setContentView(android.view.Window, Window)
    * @see #addContentView(android.view.Window, Window, android.view.ViewGroup.LayoutParams)
    */
   public void registerNEWTWindow(Window newtWindow) {
       newtWindows.add(newtWindow);
   }
   
   /**
    * Convenient method to set the Android window's flags to fullscreen or size-layout depending on the given NEWT window. 
    * <p>
    * Must be called before creating the view and adding any content, i.e. setContentView() !
    * </p>
    * @param androidWindow
    * @param newtWindow
    */
   public void layoutForNEWTWindow(android.view.Window androidWindow, Window newtWindow) {
        if(null == androidWindow || null == newtWindow) {
            throw new IllegalArgumentException("Android or NEWT Window null");
        }
        
        if( newtWindow.isFullscreen() || newtWindow.isUndecorated() ) {
            androidWindow.requestFeature(android.view.Window.FEATURE_NO_TITLE);
        }
        if( newtWindow.isFullscreen() ) {
            androidWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            androidWindow.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        } else {
            androidWindow.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            androidWindow.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);                
        }
        
        if(newtWindow.getWidth()>0 && newtWindow.getHeight()>0 && !newtWindow.isFullscreen()) {            
            androidWindow.setLayout(newtWindow.getWidth(), newtWindow.getHeight());
        }       
   }

   /**
    * Convenient method to set the Android window's flags to fullscreen or size-layout depending on the given NEWT window. 
    * <p>
    * Must be called before creating the view and adding any content, i.e. setContentView() !
    * </p>
    * @param androidWindow
    * @param newtWindow
    */
   public void setFullscreenFeature(android.view.Window androidWindow, boolean fullscreen) {
        if(null == androidWindow) {
            throw new IllegalArgumentException("Android or Window null");
        }
        
        if( fullscreen ) {
            androidWindow.requestFeature(android.view.Window.FEATURE_NO_TITLE);
            androidWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            androidWindow.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        } else {
            androidWindow.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            androidWindow.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
   }
   
   /**
    * Convenient method to set this context's theme to transparency depending on {@link CapabilitiesImmutable#isBackgroundOpaque()}. 
    * <p>
    * Must be called before creating the view and adding any content, i.e. setContentView() !
    * </p>
    */
   protected void adaptTheme4Transparency(CapabilitiesImmutable caps) {
        if(!caps.isBackgroundOpaque()) {
            setTransparencyTheme();
        }
   }
   
   /**
    * Convenient method to set this context's theme to transparency.
    * <p>
    * Must be called before creating the view and adding any content, i.e. setContentView() !
    * </p>
    * <p>
    * Is normally issued by {@link #setContentView(android.view.Window, Window)}
    * if the requested NEWT Capabilities ask for transparency.
    * </p>
    * <p>
    * Can be called only once.
    * </p>  
    */
   public void setTransparencyTheme() {
       if(!setThemeCalled) {
           setThemeCalled = true;
           final Context ctx = getActivity().getApplicationContext();            
           final String frn = ctx.getPackageName()+":style/Theme.Transparent";
           final int resID = ctx.getResources().getIdentifier("Theme.Transparent", "style", ctx.getPackageName());
           if(0 == resID) {
               Log.d(MD.TAG, "SetTransparencyTheme: Resource n/a: "+frn);
           } else {
               Log.d(MD.TAG, "SetTransparencyTheme: Setting style: "+frn+": 0x"+Integer.toHexString(resID));
               ctx.setTheme(resID);
           }
       }
   }
   
   public void setAnimator(Animator animator) {
       this.animator = animator;
       if(!animator.isStarted()) {
           animator.start();
       }
       animator.pause();
   }
      
   @Override
   public android.view.Window getWindow() {
       return getActivity().getWindow();
   }
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
       Log.d(MD.TAG, "onCreate");
       if(!isDelegatedActivity()) {
           super.onCreate(savedInstanceState);
       }
       jogamp.common.os.android.StaticContext.init(rootActivity.getApplicationContext());
   }
   
   @Override
   public void onStart() {
     Log.d(MD.TAG, "onStart");
     if(!isDelegatedActivity()) {
         super.onStart();
     }
   }
     
   @Override
   public void onRestart() {
     Log.d(MD.TAG, "onRestart");
     if(!isDelegatedActivity()) {
         super.onRestart();
     }
   }

   @Override
   public void onResume() {
     Log.d(MD.TAG, "onResume");
     if(!isDelegatedActivity()) {
         super.onResume();
     }
     for(int i=0; i<newtWindows.size(); i++) {
         final Window win = newtWindows.get(i);
         win.setVisible(true);
         if(win instanceof FPSCounter) {
             ((FPSCounter)win).resetFPSCounter();
         }
     }
     if(null != animator) {
         animator.resume();
         animator.resetFPSCounter();
     }
   }

   @Override
   public void onPause() {
     Log.d(MD.TAG, "onPause");
     if(null != animator) {
         animator.pause();
     }
     for(int i=0; i<newtWindows.size(); i++) {
         final Window win = newtWindows.get(i);
         win.setVisible(false);
     }
     if(!isDelegatedActivity()) {
         super.onPause();
     }
   }

   @Override
   public void onStop() {
     Log.d(MD.TAG, "onStop");
     if(!isDelegatedActivity()) {
         super.onStop();  
     }
   }

   @Override
   public void onDestroy() {
     Log.d(MD.TAG, "onDestroy");
     if(null != animator) {
         animator.stop();
         animator = null;
     }
     while(newtWindows.size()>0) {
         final Window win = newtWindows.remove(newtWindows.size()-1);
         win.destroy();
     }
     jogamp.common.os.android.StaticContext.clear();
     if(!isDelegatedActivity()) {
         super.onDestroy(); 
     }
   }   
}
