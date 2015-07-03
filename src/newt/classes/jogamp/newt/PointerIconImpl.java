/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package jogamp.newt;

import java.nio.ByteBuffer;

import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.nativewindow.util.PixelRectangle;
import com.jogamp.nativewindow.util.PointImmutable;

import com.jogamp.newt.Display;
import com.jogamp.newt.Display.PointerIcon;

public class PointerIconImpl implements PointerIcon {
    private final DisplayImpl display;
    private final PixelFormat pixelformat;
    private final DimensionImmutable size;
    private final ByteBuffer pixels;
    private final PointImmutable hotspot;
    private long handle;
    private int hashCode = 0;
    private volatile boolean hashCodeComputed = false;

    public PointerIconImpl(final DisplayImpl display, final PixelFormat pixelformat, final DimensionImmutable size, final ByteBuffer pixels, final PointImmutable hotspot, final long handle) {
        this.display = display;
        this.pixelformat = pixelformat;
        this.size = size;
        this.pixels = pixels;
        this.hotspot = hotspot;

        this.handle=handle;
    }
    public PointerIconImpl(final DisplayImpl display, final PixelRectangle pixelrect, final PointImmutable hotspot, final long handle) {
        this.display = display;
        this.pixelformat = pixelrect.getPixelformat();
        this.size = pixelrect.getSize();
        this.pixels = pixelrect.getPixels();
        this.hotspot = hotspot;
        this.handle=handle;
    }

    @Override
    public int hashCode() {
        if( !hashCodeComputed ) { // DBL CHECKED OK VOLATILE
            synchronized (this) {
                if( !hashCodeComputed ) {
                    // 31 * x == (x << 5) - x
                    int hash = 31 + display.getFQName().hashCode();
                    hash = ((hash << 5) - hash) + pixelformat.hashCode();
                    hash = ((hash << 5) - hash) + size.hashCode();
                    hash = ((hash << 5) - hash) + getStride();
                    hash = ((hash << 5) - hash) + ( isGLOriented() ? 1 : 0);
                    hash = ((hash << 5) - hash) + pixels.hashCode();
                    hashCode = ((hash << 5) - hash) + hotspot.hashCode();
                }
            }
        }
        return hashCode;
    }

    public synchronized final long getHandle() { return handle; }
    public synchronized final long validatedHandle() {
        synchronized(display.pointerIconList) {
            if( !display.pointerIconList.contains(this) ) {
                display.pointerIconList.add(this);
            }
        }
        if( 0 == handle ) {
            try {
                handle = display.createPointerIconImpl(pixelformat, size.getWidth(), size.getHeight(), pixels, hotspot.getX(), hotspot.getY());
                return handle;
            } catch (final Exception e) {
                e.printStackTrace();
                return 0;
            }
        } else {
            return handle;
        }
    }
    @Override
    public final Display getDisplay() { return display; }
    @Override
    public final PixelFormat getPixelformat() { return pixelformat; }
    @Override
    public final ByteBuffer getPixels() { return pixels; }
    @Override
    public synchronized final boolean isValid() { return 0 != handle; }
    @Override
    public synchronized final boolean validate() {
        if( 0 == handle ) {
            return 0 != validatedHandle();
        }
        return true;
    }

    @Override
    public synchronized void destroy() {
        if(Display.DEBUG) {
            System.err.println("PointerIcon.destroy: "+this+", "+display+", "+Display.getThreadName());
        }
        if( 0 != handle ) {
            synchronized(display.pointerIconList) {
                display.pointerIconList.remove(this);
            }
            display.runOnEDTIfAvail(false, new Runnable() {
                public void run() {
                    if( !display.isNativeValidAsync() ) {
                        destroyOnEDT(display.getHandle());
                    }
                } } );
        }
    }

    /** No checks, assume execution on EDT */
    synchronized void destroyOnEDT(final long dpy) {
        final long h = handle;
        handle = 0;
        try {
            display.destroyPointerIconImpl(dpy, h);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public final DimensionImmutable getSize() {
        return size;
    }
    @Override
    public final int getStride() {
        return size.getWidth() * pixelformat.comp.bytesPerPixel();
    }
    @Override
    public final boolean isGLOriented() {
        return false;
    }
    @Override
    public final PointImmutable getHotspot() {
        return hotspot;
    }
    @Override
    public final String toString() {
        return "PointerIcon[obj 0x"+Integer.toHexString(super.hashCode())+", "+display.getFQName()+", 0x"+Long.toHexString(handle)+", "+pixelformat+", "+size+", "+hotspot+", pixels "+pixels+"]";
    }
}