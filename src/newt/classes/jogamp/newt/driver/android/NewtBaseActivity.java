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

import com.jogamp.newt.Window;
import com.jogamp.opengl.util.Animator;

import jogamp.newt.driver.android.AndroidWindow;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class NewtBaseActivity extends Activity {
   AndroidWindow newtWindow = null;
   Animator animator = null;
    
   boolean isInvokedByExternalActivity = false;
   Activity extActivity = this;
   
   public void setIsInvokedByExternalActivity(Activity extActivity) {
       this.extActivity = extActivity;
       this.isInvokedByExternalActivity = null != extActivity;
   }
   public boolean getIsInvokedByExternalActivity() {
       return null != extActivity;
   }
   
   public Activity getActivity() {
       if(isInvokedByExternalActivity) {
           return extActivity;
       } else {
           return this;               
       }
   }     
   
   public void setContentView(android.view.Window androidWindow, Window newtWindow) {
       newtWindow = newtWindow.getDelegatedWindow();
       if(newtWindow instanceof AndroidWindow) {
           this.newtWindow = (AndroidWindow)newtWindow;
           this.newtWindow.setAndroidWindow(androidWindow);
           if(isInvokedByExternalActivity) {
               extActivity.setContentView(this.newtWindow.getAndroidView());
           } else {
               super.setContentView(this.newtWindow.getAndroidView());               
           }
       } else {
           throw new IllegalArgumentException("Given NEWT Window is not an Android Window: "+newtWindow.getClass()); 
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
       if(!isInvokedByExternalActivity) {
           super.onCreate(savedInstanceState);
           // register application context 
           jogamp.common.os.android.StaticContext.setContext(getApplicationContext());
       } else {
           jogamp.common.os.android.StaticContext.setContext(extActivity.getApplicationContext());
       }
       extActivity.getWindow();
   }
   
   @Override
   public void onStart() {
     Log.d(MD.TAG, "onStart");
     if(!isInvokedByExternalActivity) {
         super.onStart();
     }
   }
     
   @Override
   public void onRestart() {
     Log.d(MD.TAG, "onRestart");
     if(!isInvokedByExternalActivity) {
         super.onRestart();
     }
   }

   @Override
   public void onResume() {
     Log.d(MD.TAG, "onResume");
     if(!isInvokedByExternalActivity) {
         super.onResume();
     }
     if(null != animator) {
         animator.resume();
     }
   }

   @Override
   public void onPause() {
     Log.d(MD.TAG, "onPause");
     if(null != animator) {
         animator.pause();
     }
     if(!isInvokedByExternalActivity) {
         super.onPause();
     }
   }

   @Override
   public void onStop() {
     Log.d(MD.TAG, "onStop");
     if(!isInvokedByExternalActivity) {
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
     if(null != newtWindow) {
         newtWindow.destroy();
         newtWindow = null;
     }
     if(!isInvokedByExternalActivity) {
         super.onDestroy(); 
     }
   }   
}
