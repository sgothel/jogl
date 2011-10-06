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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.util.Log;

public abstract class NEWTLauncherActivity extends Activity {
   static final String TAG = "NEWTLauncherActivity";
   TextView tv = null;
   Method mOnCreate, mOnDestroy, mOnPause, mOnRestart, mOnResume, 
          mOnStart, mOnStop, mSetIsInvokedByExternalActivity;
   Class<?> activityClazz = null;
   Object activityObject  = null;
   
   public abstract String getUserActivityName();
   public abstract String getUserPackageName();
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
       Log.d(TAG, "onCreate - S");
       super.onCreate(savedInstanceState);
       
       System.setProperty("jogamp.debug.JNILibLoader", "true");
       System.setProperty("jogamp.debug.NativeLibrary", "true");
       // System.setProperty("jogamp.debug.NativeLibrary.Lookup", "true");
       // System.setProperty("jogamp.debug.IOUtil", "true");       

       // System.setProperty("nativewindow.debug", "all");
       System.setProperty("nativewindow.debug.GraphicsConfiguration", "true");

       // System.setProperty("jogl.debug", "all");
       // System.setProperty("jogl.debug.GLProfile", "true");
       System.setProperty("jogl.debug.GLDrawable", "true");
       System.setProperty("jogl.debug.GLContext", "true");
       System.setProperty("jogl.debug.GLSLCode", "true");
       System.setProperty("jogl.debug.CapabilitiesChooser", "true");
       
       // System.setProperty("jogl.debug.GLSLState", "true");
       // System.setProperty("jogl.debug.DebugGL", "true");
       // System.setProperty("jogl.debug.TraceGL", "true");

       // System.setProperty("newt.debug", "all");
       System.setProperty("newt.debug.Window", "true");
       // System.setProperty("newt.debug.Window.MouseEvent", "true");
       // System.setProperty("newt.debug.Window.KeyEvent", "true");

       ClassLoader cl = ClassLoaderUtil.createJogampClassLoaderSingleton(this, getUserPackageName());
       if(null != cl) {
           try {
               activityClazz = Class.forName(getUserActivityName(), true, cl);
               Log.d(TAG, "Activity Clazz "+activityClazz);
               activityObject = createInstance(activityClazz, null);
               Log.d(TAG, "Activity Object "+activityObject);
               mOnCreate = activityClazz.getMethod("onCreate", Bundle.class);
               mOnDestroy = activityClazz.getMethod("onDestroy");
               mOnPause = activityClazz.getMethod("onPause");
               mOnRestart = activityClazz.getMethod("onRestart");
               mOnResume = activityClazz.getMethod("onResume");
               mOnStart = activityClazz.getMethod("onStart");
               mOnStop = activityClazz.getMethod("onStop");
               mSetIsInvokedByExternalActivity = activityClazz.getMethod("setIsInvokedByExternalActivity", Activity.class);
           } catch (Exception e) {
               Log.d(TAG, "error: "+e, e);
               throw new RuntimeException(e);
           }
       }

       if( null == mOnCreate || null == mOnDestroy || null == mOnPause ||
           null == mOnRestart || null == mOnResume ||
           null == mSetIsInvokedByExternalActivity ) {
           RuntimeException e = new RuntimeException("XXX - incomplete method set");
           Log.d(TAG, "error: "+e, e);
           throw e;
       }
       
       callMethod(activityObject, mSetIsInvokedByExternalActivity, this);
       
       callMethod(activityObject, mOnCreate, savedInstanceState);
       Log.d(TAG, "onCreate - X");
   }
   
   @Override
   public void onStart() {
     Log.d(TAG, "onStart - S");
     callMethod(activityObject, mOnStart);
     super.onStart();
     Log.d(TAG, "onStart - X");
   }
     
   @Override
   public void onRestart() {
     Log.d(TAG, "onRestart - S");
     callMethod(activityObject, mOnRestart);
     super.onRestart();
     Log.d(TAG, "onRestart - X");
   }

   @Override
   public void onResume() {
     Log.d(TAG, "onResume - S");
     callMethod(activityObject, mOnResume);
     super.onResume();
     Log.d(TAG, "onResume - X");
   }

   @Override
   public void onPause() {
     Log.d(TAG, "onPause - S");
     callMethod(activityObject, mOnPause);
     super.onPause();
     Log.d(TAG, "onPause - X");
   }

   @Override
   public void onStop() {
     Log.d(TAG, "onStop - S");
     callMethod(activityObject, mOnStop);
     super.onStop();  
     Log.d(TAG, "onStop - X");
   }

   @Override
   public void onDestroy() {
     Log.d(TAG, "onDestroy - S");
     callMethod(activityObject, mOnDestroy);
     super.onDestroy();  
     finish();
     Log.d(TAG, "onDestroy - X");
   }   

   @Override
   public void finish() {
     Log.d(TAG, "finish - S");
     super.finish();  
     Log.d(TAG, "finish - X");
   }   

  /**
   * @throws JogampRuntimeException if the instance can not be created.
   */
  public static final Object createInstance(Class<?> clazz, Class<?>[] cstrArgTypes, Object ... cstrArgs) 
      throws RuntimeException
  {
    return createInstance(getConstructor(clazz, cstrArgTypes), cstrArgs);
  }

  public static final Object createInstance(Constructor<?> cstr, Object ... cstrArgs) 
      throws RuntimeException
  {
    try {
        return cstr.newInstance(cstrArgs);
    } catch (Exception e) {
      Throwable t = e;
      if (t instanceof InvocationTargetException) {
        t = ((InvocationTargetException) t).getTargetException();
      }
      if (t instanceof Error) {
        throw (Error) t;
      }
      if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      }
      throw new RuntimeException("can not create instance of "+cstr.getName(), t);
    }
  }
  
    /**
     * @throws JogampRuntimeException if the constructor can not be delivered.
     */
    protected static final Constructor<?> getConstructor(Class<?> clazz, Class<?> ... cstrArgTypes) 
        throws RuntimeException {
        try {
            if(null == cstrArgTypes) {
                cstrArgTypes = zeroTypes;
            }
            return clazz.getDeclaredConstructor(cstrArgTypes);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("Constructor: '" + clazz + "(" + asString(cstrArgTypes) + ")' not found", ex);
        }
    }
    
    protected static final Class<?>[] zeroTypes = new Class[0];

    protected static final String asString(Class<?>[] argTypes) {
        StringBuffer args = new StringBuffer();
        boolean coma = false;
        if(null != argTypes) {
            for (int i = 0; i < argTypes.length; i++) {
                if(coma) {
                     args.append(", ");
                }
                args.append(argTypes[i].getName());
                coma = true;
            }
        }
        return args.toString();
    }
    
  protected static final Object callMethod(Object instance, Method method, Object ... args)
      throws RuntimeException
  {
    try {
        return method.invoke(instance, args);
    } catch (Exception e) {
      Throwable t = e;
      if (t instanceof InvocationTargetException) {
        t = ((InvocationTargetException) t).getTargetException();
      }
      if (t instanceof Error) {
        throw (Error) t;
      }
      if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      }
      throw new RuntimeException("calling "+method+" failed", t);
    }
  }


}
