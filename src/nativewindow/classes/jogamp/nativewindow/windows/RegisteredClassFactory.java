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

package jogamp.nativewindow.windows;

import jogamp.nativewindow.Debug;

import java.util.ArrayList;

import com.jogamp.nativewindow.NativeWindowException;

public class RegisteredClassFactory {
    private static final boolean DEBUG = Debug.debug("RegisteredClass");
    private static final ArrayList<RegisteredClassFactory> registeredFactories;
    private static final long hInstance;

    static {
        hInstance = GDI.GetApplicationHandle();
        if( 0 == hInstance ) {
            throw new NativeWindowException("Error: Null ModuleHandle for Application");
        }
        registeredFactories = new ArrayList<RegisteredClassFactory>();
    }

    private final String classBaseName;
    private final long wndProc;
    private final boolean useDummyDispatchThread;
    private final long iconSmallHandle, iconBigHandle;

    private RegisteredClass sharedClass = null;
    private int classIter = 0;
    private int sharedRefCount = 0;
    private final Object sync = new Object();

    private String toHexString(final long l) { return "0x"+Long.toHexString(l); }

    @Override
    public final String toString() { return "RegisteredClassFactory[moduleHandle "+toHexString(hInstance)+", "+classBaseName+
            ", wndProc "+toHexString(wndProc)+", useDDT "+useDummyDispatchThread+", shared[refCount "+sharedRefCount+", class "+sharedClass+"]]"; }

    /**
     * Release the {@link RegisteredClass} of all {@link RegisteredClassFactory}.
     */
    public static void shutdownSharedClasses() {
        synchronized(registeredFactories) {
            if( DEBUG ) {
                System.err.println("RegisteredClassFactory.shutdownSharedClasses: "+registeredFactories.size());
            }
            for(int j=0; j<registeredFactories.size(); j++) {
                final RegisteredClassFactory rcf = registeredFactories.get(j);
                synchronized(rcf.sync) {
                    if(null != rcf.sharedClass) {
                        GDIUtil.DestroyWindowClass0(rcf.sharedClass.getHInstance(), rcf.sharedClass.getName(), rcf.sharedClass.getHDispThreadContext());
                        rcf.sharedClass = null;
                        rcf.sharedRefCount = 0;
                        rcf.classIter = 0;
                        if(DEBUG) {
                            System.err.println("RegisteredClassFactory #"+j+"/"+registeredFactories.size()+": shutdownSharedClasses : "+rcf.sharedClass);
                        }
                    } else if(DEBUG) {
                        System.err.println("RegisteredClassFactory #"+j+"/"+registeredFactories.size()+": null");
                    }
                }
            }
        }
    }

    /** Application handle. */
    public static long getHInstance() { return hInstance; }

    public RegisteredClassFactory(final String classBaseName, final long wndProc, final boolean useDummyDispatchThread, final long iconSmallHandle, final long iconBigHandle) {
        this.classBaseName = classBaseName;
        this.wndProc = wndProc;
        this.useDummyDispatchThread = useDummyDispatchThread;
        this.iconSmallHandle = iconSmallHandle;
        this.iconBigHandle = iconBigHandle;
        synchronized(registeredFactories) {
            registeredFactories.add(this);
        }
    }

    public RegisteredClass getSharedClass() throws NativeWindowException {
      synchronized(sync) {
          if( 0 == sharedRefCount ) {
              if( null != sharedClass ) {
                  throw new InternalError("Error ("+sharedRefCount+"): SharedClass not null: "+sharedClass);
              }
              String clazzName = null;
              boolean registered = false;
              final int classIterMark = classIter - 1;
              while ( !registered && classIterMark != classIter ) {
                  // Retry with next clazz name, this could happen if more than one JVM is running
                  clazzName = classBaseName + classIter;
                  classIter++;
                  registered = GDIUtil.CreateWindowClass0(hInstance, clazzName, wndProc, iconSmallHandle, iconBigHandle);
              }
              if( !registered ) {
                  throw new NativeWindowException("Error: Could not create WindowClass: "+clazzName);
              }
              final long hDispatchThread;
              if( useDummyDispatchThread ) {
                  hDispatchThread = GDIUtil.CreateDummyDispatchThread0();
                  if( 0 == hDispatchThread ) {
                      throw new NativeWindowException("Error: Could not create DDT "+clazzName);
                  }
              } else {
                  hDispatchThread = 0;
              }
              sharedClass = new RegisteredClass(hInstance, clazzName, hDispatchThread);
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
              GDIUtil.DestroyWindowClass0(sharedClass.getHInstance(), sharedClass.getName(), sharedClass.getHDispThreadContext());
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
