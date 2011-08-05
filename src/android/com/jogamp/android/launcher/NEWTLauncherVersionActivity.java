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
package com.jogamp.android.launcher;

import java.lang.reflect.Method;

import dalvik.system.PathClassLoader;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.util.Log;

public class NEWTLauncherVersionActivity extends Activity {
   static final String TAG = "JoglLauncherActivity";
   TextView tv = null;
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
       Log.d(TAG, "onCreate - S");
       super.onCreate(savedInstanceState);
       
       String packageGlueGen = "com.jogamp.common";       
       String apkGlueGen = null;
       String packageJogl = "javax.media.opengl";
       String apkJogl = null;
       
       String clazzMDName= "jogamp.newt.driver.android.MD";
       Method mdGetInfo = null;

       try {
           apkGlueGen = getPackageManager().getApplicationInfo(packageGlueGen,0).sourceDir;
           apkJogl = getPackageManager().getApplicationInfo(packageJogl,0).sourceDir;
       } catch (PackageManager.NameNotFoundException e) {
           Log.d(TAG, "error: "+e, e);
       }
       if(null == apkGlueGen || null == apkJogl) {
           Log.d(TAG, "not found: gluegen <"+apkGlueGen+">, jogl <"+apkJogl+">");
       } else {
           String cp = apkGlueGen + ":" + apkJogl ;
           Log.d(TAG, "cp: " + cp);
            
           // add path to apk that contains classes you wish to load
           PathClassLoader pathClassLoader = new dalvik.system.PathClassLoader(
                    cp,
                    ClassLoader.getSystemClassLoader());
        
           try {
               Class clazzMD= Class.forName(clazzMDName, true, pathClassLoader);
               Log.d(TAG, "MD: "+clazzMD);
               mdGetInfo = clazzMD.getMethod("getInfo");
           } catch (Exception e) {
               Log.d(TAG, "error: "+e, e);
           }
       }

       String mdInfo = null;
       try {
           mdInfo = (String) mdGetInfo.invoke(null);
       } catch (Exception e) {
           Log.d(TAG, "error: "+e, e);
       }
       tv = new TextView(this);
       if(null != mdInfo) {
           tv.setText(mdInfo);
       } else {
           tv.setText("mdInfo n/a");
       }
       setContentView(tv);
       Log.d(TAG, "onCreate - X");
   }
   
   @Override
   public void onStart() {
     Log.d(TAG, "onStart - S");
     super.onStart();
     Log.d(TAG, "onStart - X");
   }
     
   @Override
   public void onRestart() {
     Log.d(TAG, "onRestart - S");
     super.onRestart();
     Log.d(TAG, "onRestart - X");
   }

   @Override
   public void onResume() {
     Log.d(TAG, "onResume - S");
     super.onResume();
     Log.d(TAG, "onResume - X");
   }

   @Override
   public void onPause() {
     Log.d(TAG, "onPause - S");
     super.onPause();
     Log.d(TAG, "onPause - X");
   }

   @Override
   public void onStop() {
     Log.d(TAG, "onStop - S");
     super.onStop();  
     Log.d(TAG, "onStop - X");
   }

   @Override
   public void onDestroy() {
     Log.d(TAG, "onDestroy - S");
     super.onDestroy();  
     Log.d(TAG, "onDestroy - X");
   }   
}
