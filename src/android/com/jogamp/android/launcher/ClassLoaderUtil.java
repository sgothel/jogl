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

import java.io.File;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import dalvik.system.DexClassLoader;

public class ClassLoaderUtil {
   private static final String TAG = "JogampClassLoader";
   
   public static final String packageGlueGen = "com.jogamp.common";       
   public static final String packageJogl = "javax.media.opengl";
   public static final String packageJoglTest = "com.jogamp.opengl.test";
   
   public static final String dexPathName= "jogampDex";
   public static final String libPathName = "/data/data/com.jogamp.common/lib/:/data/data/javax.media.opengl/lib/";
   
   public static synchronized ClassLoader createJogampClassLoaderSingleton(Context ctx) {
       Log.d(TAG, "S");
       
       String apkGlueGen = null;
       String apkJogl = null;
       String apkJoglTest = null;
       
       try {
           apkGlueGen = ctx.getPackageManager().getApplicationInfo(packageGlueGen,0).sourceDir;
           apkJogl = ctx.getPackageManager().getApplicationInfo(packageJogl,0).sourceDir;
           apkJoglTest = ctx.getPackageManager().getApplicationInfo(packageJoglTest,0).sourceDir;
       } catch (PackageManager.NameNotFoundException e) {
           Log.d(TAG, "error: "+e, e);
       }
       if(null == apkGlueGen || null == apkJogl || null == apkJoglTest) {
           Log.d(TAG, "not found: gluegen <"+apkGlueGen+">, jogl <"+apkJogl+">, jogl-test <"+apkJoglTest+">");
           return null;
       }
       
       final String cp = apkGlueGen + ":" + apkJogl + ":" + apkJoglTest ;
       Log.d(TAG, "cp: " + cp);
       
       final File dexPath = ctx.getDir(dexPathName, Context.MODE_WORLD_READABLE);
       
       ClassLoader cl = new DexClassLoader(cp, dexPath.getAbsolutePath(), libPathName, ctx.getClassLoader());
       Log.d(TAG, "cl: " + cl);
       // setAPKClassLoader(dexLoader);
       
       return cl;
   }

}
