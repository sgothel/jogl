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

package com.jogamp.nativewindow;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.ProxySurface;

public class WrappedSurface extends ProxySurface {
  protected long surfaceHandle;  

  public WrappedSurface(AbstractGraphicsConfiguration cfg, long handle, int initialWidth, int initialHeight, UpstreamSurfaceHook upstream) {
    super(cfg, initialWidth, initialHeight, upstream);
    surfaceHandle=handle;
  }

  @Override
  protected void invalidateImpl() {    
    surfaceHandle = 0;
  }

  @Override
  public final long getSurfaceHandle() {
    return surfaceHandle;
  }

  @Override
  public final void setSurfaceHandle(long surfaceHandle) {
    this.surfaceHandle=surfaceHandle;
  }
  
  @Override
  protected final int lockSurfaceImpl() {
    return LOCK_SUCCESS;
  }

  @Override
  protected final void unlockSurfaceImpl() {
  }

  @Override
  public String toString() {
    final UpstreamSurfaceHook ush = getUpstreamSurfaceHook();
    final String ush_s = null != ush ? ( ush.getClass().getName() + ": " + ush ) : "nil"; 
    
    return "WrappedSurface[config " + getPrivateGraphicsConfiguration()+
           ", displayHandle 0x" + Long.toHexString(getDisplayHandle()) +
           ", surfaceHandle 0x" + Long.toHexString(getSurfaceHandle()) +
           ", size " + getWidth() + "x" + getHeight() +
           ", surfaceLock "+surfaceLock+
           ", upstreamSurfaceHook "+ush_s+"]";
  }
}
