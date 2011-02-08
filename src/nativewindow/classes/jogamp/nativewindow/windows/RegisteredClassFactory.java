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

package com.jogamp.nativewindow.impl.windows;

import com.jogamp.nativewindow.impl.Debug;
import java.util.ArrayList;
import javax.media.nativewindow.NativeWindowException;

public class RegisteredClassFactory {
    static final boolean DEBUG = Debug.debug("RegisteredClass");
    private static ArrayList sharedClasses = new ArrayList();
    private String classBaseName;
    long wndProc;

    private RegisteredClass sharedClass = null;
    private int classIter = 0;
    private int sharedRefCount = 0;
    private Object sync = new Object();

    /**
     * Intended for a JVM shutdown hook, hence little synchronization
     */
    public static void shutdownSharedClasses() {
        synchronized(sharedClasses) {
            for(int i=0; i<sharedClasses.size(); i++) {
                RegisteredClass sc = (RegisteredClass) sharedClasses.get(i);
                GDI.DestroyWindowClass(sc.getHandle(), sc.getName());
                if(DEBUG) {
                  System.err.println("RegisteredClassFactory shutdownSharedClasses "+i+"/"+sharedClasses.size()+": "+sc);
                }
            }
            sharedClasses.clear();
        }
    }

    public RegisteredClassFactory(String classBaseName, long wndProc) {
        this.classBaseName = classBaseName;
        this.wndProc = wndProc;
    }

    public RegisteredClass getSharedClass() throws NativeWindowException {
      synchronized(sync) {
          if( 0 == sharedRefCount ) {
              if( null != sharedClass ) {
                  throw new InternalError("Error ("+sharedRefCount+"): SharedClass not null: "+sharedClass);
              }
              long hInstance = GDI.GetApplicationHandle();
              if( 0 == hInstance ) {
                  throw new NativeWindowException("Error: Null ModuleHandle for Application");
              }
              String clazzName = null;
              boolean registered = false;
              while ( !registered && Integer.MAX_VALUE >= classIter ) {
                  // Retry with next clazz name, this could happen if more than one JVM is running
                  clazzName = classBaseName + classIter;
                  classIter++;
                  registered = GDI.CreateWindowClass(hInstance, clazzName, wndProc);
              }
              if( !registered ) {
                  throw new NativeWindowException("Error: Could not create WindowClass: "+clazzName);
              }
              sharedClass = new RegisteredClass(hInstance, clazzName);
              synchronized(sharedClasses) {
                  sharedClasses.add(sharedClass);
              }
              if(DEBUG) {
                  System.err.println("RegisteredClassFactory getSharedClass ("+sharedRefCount+") initialized: "+sharedClass);
              }
          } else if ( null == sharedClass ) {
              throw new InternalError("Error ("+sharedRefCount+"): SharedClass is null");
          }
          sharedRefCount++;
      }
      return sharedClass;
    }

    public void releaseSharedClass() {
      synchronized(sync) {
          if( 0 == sharedRefCount ) {
              if( null != sharedClass ) {
                  throw new InternalError("Error ("+sharedRefCount+"): SharedClass not null: "+sharedClass);
              }
              return;
          }
          sharedRefCount--;
          if( null == sharedClass ) {
              throw new InternalError("Error ("+sharedRefCount+"): SharedClass is null");
          }
          if( 0 == sharedRefCount ) {
              GDI.DestroyWindowClass(sharedClass.getHandle(), sharedClass.getName());
              synchronized(sharedClasses) {
                  sharedClasses.remove(sharedClass);
              }
              if(DEBUG) {
                  System.err.println("RegisteredClassFactory releaseSharedClass ("+sharedRefCount+") released: "+sharedClass);
              }
              sharedClass = null;
              sharedRefCount = 0;
              classIter = 0;
          }
      }
    }

    public int getSharedRefCount() {
        return sharedRefCount;
    }
}
