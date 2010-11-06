/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package javax.media.nativewindow;

import com.jogamp.nativewindow.impl.NativeWindowFactoryImpl;

public class DefaultGraphicsDevice implements Cloneable, AbstractGraphicsDevice {
    private String type;
    protected long handle;
    protected ToolkitLock toolkitLock;

    /**
     * Create an instance with the system default {@link ToolkitLock},
     * gathered via {@link NativeWindowFactory#createDefaultToolkitLock()}.
     * @param type
     */
    public DefaultGraphicsDevice(String type) {
        this.type = type;
        this.handle = 0;
        setToolkitLock( NativeWindowFactory.getDefaultToolkitLock(type) );
    }

    /**
     * Create an instance with the system default {@link ToolkitLock}.
     * gathered via {@link NativeWindowFactory#createDefaultToolkitLock()}.
     * @param type
     * @param handle
     */
    public DefaultGraphicsDevice(String type, long handle) {
        this.type = type;
        this.handle = handle;
        setToolkitLock( NativeWindowFactory.createDefaultToolkitLock(type, handle) );
    }

    /**
     * Create an instance with the given {@link ToolkitLock} instance.
     * @param type
     * @param handle
     * @param locker
     */
    public DefaultGraphicsDevice(String type, long handle, ToolkitLock locker) {
        this.type = type;
        this.handle = handle;
        setToolkitLock( locker );
    }

    public Object clone() {
        try {
          return super.clone();
        } catch (CloneNotSupportedException e) {
          throw new NativeWindowException(e);
        }
    }

    public String getType() {
        return type;
    }

    public long getHandle() {
        return handle;
    }

    /**
     * No lock is performed on the graphics device per default,
     * instead the aggregated recursive {@link ToolkitLock#lock()} is invoked.
     *
     * @see DefaultGraphicsDevice#DefaultGraphicsDevice(java.lang.String, long)
     * @see DefaultGraphicsDevice#DefaultGraphicsDevice(java.lang.String, long, javax.media.nativewindow.ToolkitLock)
     */
    public final void lock() {
        toolkitLock.lock();
    }

    /** 
     * No lock is performed on the graphics device per default,
     * instead the aggregated recursive {@link ToolkitLock#unlock()} is invoked.
     *
     * @see DefaultGraphicsDevice#DefaultGraphicsDevice(java.lang.String, long)
     * @see DefaultGraphicsDevice#DefaultGraphicsDevice(java.lang.String, long, javax.media.nativewindow.ToolkitLock)
     */
    public final void unlock() {
        toolkitLock.unlock();
    }

    public boolean close() {
        return false;
    }

    public String toString() {
        return getClass().toString()+"[type "+getType()+", handle 0x"+Long.toHexString(getHandle())+"]";
    }

    /**
     * Set the internal ToolkitLock, which is used within the
     * {@link #lock()} and {@link #unlock()} implementation.
     *
     * @param locker the ToolkitLock, if null, {@link com.jogamp.nativewindow.impl.NullToolkitLock} is being used
     */
    protected void setToolkitLock(ToolkitLock locker) {
        this.toolkitLock = ( null == locker ) ? NativeWindowFactoryImpl.getNullToolkitLock() : locker ;
    }

    /**
     * @return the used ToolkitLock
     *
     * @see DefaultGraphicsDevice#DefaultGraphicsDevice(java.lang.String, long)
     * @see DefaultGraphicsDevice#DefaultGraphicsDevice(java.lang.String, long, javax.media.nativewindow.ToolkitLock)
     */
    public final ToolkitLock getToolkitLock() {
         return toolkitLock;
    }
}
