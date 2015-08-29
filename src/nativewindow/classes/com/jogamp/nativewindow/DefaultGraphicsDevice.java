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

package com.jogamp.nativewindow;

import jogamp.nativewindow.NativeWindowFactoryImpl;

public class DefaultGraphicsDevice implements Cloneable, AbstractGraphicsDevice {
    private static final String separator = "_";
    private final String type;
    protected final String connection;
    protected final int unitID;
    protected final String uniqueID;
    protected long handle;
    protected ToolkitLock toolkitLock;

    /**
     * Return the default display connection for the given windowing toolkit type
     * gathered via {@link NativeWindowFactory#getDefaultDisplayConnection()}.
     * @param type
     */
    public static String getDefaultDisplayConnection() {
        return NativeWindowFactory.getDefaultDisplayConnection();
    }
    /**
     * Return the default display connection for the given windowing toolkit type
     * gathered via {@link NativeWindowFactory#getDefaultDisplayConnection(String)}.
     * @param type
     */
    public static String getDefaultDisplayConnection(final String type) {
        return NativeWindowFactory.getDefaultDisplayConnection(type);
    }

    /**
     * Create an instance with the system default {@link ToolkitLock},
     * gathered via {@link NativeWindowFactory#getDefaultToolkitLock(String)}.
     * @param type
     */
    public DefaultGraphicsDevice(final String type, final String connection, final int unitID) {
        this(type, connection, unitID, 0, NativeWindowFactory.getDefaultToolkitLock(type));
    }

    /**
     * Create an instance with the system default {@link ToolkitLock}.
     * gathered via {@link NativeWindowFactory#getDefaultToolkitLock(String, long)}.
     * @param type
     * @param handle
     */
    public DefaultGraphicsDevice(final String type, final String connection, final int unitID, final long handle) {
        this(type, connection, unitID, handle, NativeWindowFactory.getDefaultToolkitLock(type, handle));
    }

    /**
     * Create an instance with the given {@link ToolkitLock} instance, or <i>null</i> {@link ToolkitLock} if null.
     * @param type
     * @param handle
     * @param locker if null, a non blocking <i>null</i> lock is used.
     */
    public DefaultGraphicsDevice(final String type, final String connection, final int unitID, final long handle, final ToolkitLock locker) {
        this.type = type;
        this.connection = connection;
        this.unitID = unitID;
        this.uniqueID = getUniqueID(type, connection, unitID);
        this.handle = handle;
        this.toolkitLock = null != locker ? locker : NativeWindowFactoryImpl.getNullToolkitLock();
    }

    @Override
    public Object clone() {
        try {
          return super.clone();
        } catch (final CloneNotSupportedException e) {
          throw new NativeWindowException(e);
        }
    }

    @Override
    public final String getType() {
        return type;
    }

    @Override
    public final String getConnection() {
        return connection;
    }

    @Override
    public final int getUnitID() {
        return unitID;
    }

    @Override
    public final String getUniqueID() {
      return uniqueID;
    }

    @Override
    public final long getHandle() {
        return handle;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Locking is perfomed via delegation to {@link ToolkitLock#lock()}, {@link ToolkitLock#unlock()}.
     * </p>
     *
     * @see DefaultGraphicsDevice#DefaultGraphicsDevice(java.lang.String, long)
     * @see DefaultGraphicsDevice#DefaultGraphicsDevice(java.lang.String, long, com.jogamp.nativewindow.ToolkitLock)
     */
    @Override
    public final void lock() {
        toolkitLock.lock();
    }

    @Override
    public final void validateLocked() throws RuntimeException {
        toolkitLock.validateLocked();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Locking is perfomed via delegation to {@link ToolkitLock#lock()}, {@link ToolkitLock#unlock()}.
     * </p>
     *
     * @see DefaultGraphicsDevice#DefaultGraphicsDevice(java.lang.String, long)
     * @see DefaultGraphicsDevice#DefaultGraphicsDevice(java.lang.String, long, com.jogamp.nativewindow.ToolkitLock)
     */
    @Override
    public final void unlock() {
        toolkitLock.unlock();
    }

    @Override
    public boolean open() {
        return false;
    }

    @Override
    public boolean close() {
        toolkitLock.dispose();
        if(0 != handle) {
            handle = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean isHandleOwner() {
        return false;
    }

    @Override
    public void clearHandleOwner() {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"[type "+getType()+", connection "+getConnection()+", unitID "+getUnitID()+", handle 0x"+Long.toHexString(getHandle())+", owner "+isHandleOwner()+", "+toolkitLock+"]";
    }

    /**
     * Set the native handle of the underlying native device
     * and return the previous one.
     */
    protected final long setHandle(final long newHandle) {
        final long oldHandle = handle;
        handle = newHandle;
        return oldHandle;
    }

    protected Object getHandleOwnership() {
        return null;
    }
    protected Object setHandleOwnership(final Object newOwnership) {
        return null;
    }

    public static final void swapDeviceHandleAndOwnership(final DefaultGraphicsDevice aDevice1, final DefaultGraphicsDevice aDevice2) {
        aDevice1.lock();
        try {
            aDevice2.lock();
            try {
                final long aDevice1Handle = aDevice1.getHandle();
                final long aDevice2Handle = aDevice2.setHandle(aDevice1Handle);
                aDevice1.setHandle(aDevice2Handle);
                final Object aOwnership1 = aDevice1.getHandleOwnership();
                final Object aOwnership2 = aDevice2.setHandleOwnership(aOwnership1);
                aDevice1.setHandleOwnership(aOwnership2);
            } finally {
                aDevice2.unlock();
            }
        } finally {
            aDevice1.unlock();
        }
    }

    /**
     * Set the internal ToolkitLock, which is used within the
     * {@link #lock()} and {@link #unlock()} implementation.
     *
     * <p>
     * The current ToolkitLock is being locked/unlocked while swapping the reference,
     * ensuring no concurrent access can occur during the swap.
     * </p>
     *
     * @param locker the ToolkitLock, if null, {@link jogamp.nativewindow.NullToolkitLock} is being used
     * @return the previous ToolkitLock instance
     */
    protected ToolkitLock setToolkitLock(final ToolkitLock locker) {
        final ToolkitLock _toolkitLock = toolkitLock;
        _toolkitLock.lock();
        try {
            toolkitLock = ( null == locker ) ? NativeWindowFactoryImpl.getNullToolkitLock() : locker ;
        } finally {
            _toolkitLock.unlock();
        }
        return _toolkitLock;
    }

    /**
     * @return the used ToolkitLock
     *
     * @see DefaultGraphicsDevice#DefaultGraphicsDevice(java.lang.String, long)
     * @see DefaultGraphicsDevice#DefaultGraphicsDevice(java.lang.String, long, com.jogamp.nativewindow.ToolkitLock)
     */
    public final ToolkitLock getToolkitLock() {
         return toolkitLock;
    }

   /**
    * Returns a unique String object using {@link String#intern()} for the given arguments,
    * which object reference itself can be used as a key.
    */
    private static String getUniqueID(final String type, final String connection, final int unitID) {
      return (type + separator + connection + separator + unitID).intern();
    }
}
