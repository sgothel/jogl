/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
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
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package javax.media.nativewindow;

/** A interface describing a graphics device in a
    toolkit-independent manner.
 */

public interface AbstractGraphicsDevice extends Cloneable {
    /** Dummy connection value for a default connection where no native support for multiple devices is available */
    public static String DEFAULT_CONNECTION = "decon";

    /** Dummy connection value for an external connection where no native support for multiple devices is available */
    public static String EXTERNAL_CONNECTION = "excon";

    /** Default unit id for the 1st device: 0 */
    public static int DEFAULT_UNIT = 0;

    /**
     * Returns the type of the underlying subsystem, ie
     * NativeWindowFactory.TYPE_KD, NativeWindowFactory.TYPE_X11, ..
     */
    public String getType();

    /**
     * Returns the semantic GraphicsDevice connection.<br>
     * On platforms supporting remote devices, eg via tcp/ip network,
     * the implementation shall return a unique name for each remote address.<br>
     * On X11 for example, the connection string should be as the following example.<br>
     * <ul>
     *   <li><code>:0.0</code> for a local connection</li>
     *   <li><code>remote.host.net:0.0</code> for a remote connection</li>
     * </ul>
     *
     * To support multiple local device, see {@link #getUnitID()}.
     */
    public String getConnection();

    /**
     * Returns the graphics device <code>unit ID</code>.<br>
     * The <code>unit ID</code> support multiple graphics device configurations
     * on a local machine.<br>
     * To support remote device, see {@link #getConnection()}.
     * @return
     */
    public int getUnitID();

    /**
     * Returns a unique ID String of this device using {@link #getType() type},
     * {@link #getConnection() connection} and {@link #getUnitID() unitID}.<br>
     * The unique ID does not reflect the instance of the device, hence the handle is not included.<br>
     * The unique ID may be used as a key for semantic device mapping.
     */
    public String getUniqueID();

    /**
     * Returns the native handle of the underlying native device,
     * if such thing exist.
     */
    public long getHandle();

    /**
     * Optionally locking the device, utilizing eg {@link javax.media.nativewindow.ToolkitLock}.
     * The lock implementation must be recursive.
     */
    public void lock();

    /** 
     * Optionally unlocking the device, utilizing eg {@link javax.media.nativewindow.ToolkitLock}.
     * The lock implementation must be recursive.
     */
    public void unlock();

    /**
     * Optionally closing the device.
     * <p>
     * The default implementation is a <code>NOP</code>, just setting the handle to <code>null</code>.
     * </p>
     * The specific implementing, ie {@link javax.media.nativewindow.x11.X11GraphicsDevice},
     * shall have a enable/disable like {@link javax.media.nativewindow.x11.X11GraphicsDevice#setCloseDisplay(boolean, boolean)},<br>
     * which shall be invoked at creation time to determine ownership/role of freeing the resource.<br>
     *
     * @return true if the handle was not <code>null</code>, otherwise false.
     */
    public boolean close();
}
