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

package com.jogamp.nativewindow;

/** A marker interface describing a graphics configuration, visual, or
    pixel format in a toolkit-independent manner. */
public interface AbstractGraphicsConfiguration extends VisualIDHolder, Cloneable {
    public Object clone();

    /**
     * Return the screen this graphics configuration is valid for
     */
    public AbstractGraphicsScreen getScreen();

    /**
     * Return the capabilities reflecting this graphics configuration,
     * which may differ from the capabilities used to choose this configuration.
     *
     * @return An immutable instance of the Capabilities to avoid mutation by
     * the user.
     */
    public CapabilitiesImmutable getChosenCapabilities();

    /**
     * Return the capabilities used to choose this graphics configuration.
     *
     * These may be used to reconfigure the NativeWindow in case
     * the device changes in a multiple screen environment.
     *
     * @return An immutable instance of the Capabilities to avoid mutation by
     * the user.
     */
    public CapabilitiesImmutable getRequestedCapabilities();

    /**
     * In case the implementation utilizes a delegation pattern to wrap abstract toolkits,
     * this method shall return the native {@link AbstractGraphicsConfiguration},
     * otherwise this instance.
     * @see NativeSurface#getGraphicsConfiguration()
     */
    public AbstractGraphicsConfiguration getNativeGraphicsConfiguration();
}

