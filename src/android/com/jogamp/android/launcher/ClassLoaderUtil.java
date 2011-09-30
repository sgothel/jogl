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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import dalvik.system.DexClassLoader;

public class ClassLoaderUtil {
   private static final String TAG = "JogampClassLoader";
   
   public static final String packageGlueGen = "com.jogamp.common";       
   public static final String packageJogl = "javax.media.opengl";
   
   public static final String dexPathName= "jogampDex";
   
   private static LauncherTempFileCache tmpFileCache = null;
   private static ClassLoader jogAmpClassLoader = null;
   
   public static synchronized ClassLoader createJogampClassLoaderSingleton(Context ctx, String userPackageName) {
       if(null==jogAmpClassLoader) {
           if(null!=tmpFileCache) {
               throw new InternalError("XXX0");
           }
           if(!LauncherTempFileCache.initSingleton(ctx)) {
               throw new InternalError("TempFileCache initialization error");
           }
           tmpFileCache = new LauncherTempFileCache();
           if(!tmpFileCache.isValid()) {
               throw new InternalError("TempFileCache instantiation error");                
           }
           final ApplicationInfo ai = ctx.getApplicationInfo();
           Log.d(TAG, "S: userPackageName: "+userPackageName+", dataDir: "+ai.dataDir+", nativeLibraryDir: "+ai.nativeLibraryDir);
    
           final String appDir = new File(ai.dataDir).getParent();
           final String libSub = ai.nativeLibraryDir.substring(ai.nativeLibraryDir.lastIndexOf('/')+1);
           Log.d(TAG, "S: appDir: "+appDir+", libSub: "+libSub);
           
           final String libPathName = appDir + "/" + packageGlueGen + "/" + libSub + "/:" +
                                      appDir + "/" + packageJogl + "/" + libSub + "/" ;
           Log.d(TAG, "S: libPath: "+libPathName);
                   
           String apkGlueGen = null;
           String apkJogl = null;
       
           try {
               apkGlueGen = ctx.getPackageManager().getApplicationInfo(packageGlueGen,0).sourceDir;
               apkJogl = ctx.getPackageManager().getApplicationInfo(packageJogl,0).sourceDir;
           } catch (PackageManager.NameNotFoundException e) {
               Log.d(TAG, "error: "+e, e);
           }
           if(null == apkGlueGen || null == apkJogl) {
               Log.d(TAG, "not found: gluegen <"+apkGlueGen+">, jogl <"+apkJogl+">");
               return null;
           }
           
           final String cp = apkGlueGen + ":" + apkJogl ;
           Log.d(TAG, "jogamp cp: " + cp);
       
           final File dexPath = new File(tmpFileCache.getTempDir(), dexPathName);
           Log.d(TAG, "jogamp dexPath: " + dexPath.getAbsolutePath());
           dexPath.mkdir();
           jogAmpClassLoader = new DexClassLoader(cp, dexPath.getAbsolutePath(), libPathName, ctx.getClassLoader());
       } else {
           if(null==tmpFileCache) {
               throw new InternalError("XXX1");
           }           
       }
       
       String apkUser = null;
       try {
           apkUser = ctx.getPackageManager().getApplicationInfo(userPackageName,0).sourceDir;
       } catch (PackageManager.NameNotFoundException e) {
           Log.d(TAG, "error: "+e, e);
       }
       if(null == apkUser) {
           Log.d(TAG, "not found: user apk <"+apkUser+">");
           return null;
       }
       
       Log.d(TAG, "user cp: " + apkUser);
       final File dexPath = new File(tmpFileCache.getTempDir(), userPackageName);
       Log.d(TAG, "user dexPath: " + dexPath.getAbsolutePath());
       dexPath.mkdir();
       ClassLoader cl = new DexClassLoader(apkUser, dexPath.getAbsolutePath(), null, jogAmpClassLoader);
       Log.d(TAG, "cl: " + cl);
       // setAPKClassLoader(dexLoader);
       
       return cl;
   }

}
