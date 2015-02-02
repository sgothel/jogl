/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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

import jogamp.nativewindow.Debug;

public class DefaultGraphicsConfiguration implements Cloneable, AbstractGraphicsConfiguration {
    protected static final boolean DEBUG = Debug.debug("GraphicsConfiguration");

    private AbstractGraphicsScreen screen;
    protected CapabilitiesImmutable capabilitiesChosen;
    protected CapabilitiesImmutable capabilitiesRequested;

    public DefaultGraphicsConfiguration(final AbstractGraphicsScreen screen,
                                        final CapabilitiesImmutable capsChosen, final CapabilitiesImmutable capsRequested) {
        if(null == screen) {
            throw new IllegalArgumentException("Null screen");
        }
        if(null == capsChosen) {
            throw new IllegalArgumentException("Null chosen caps");
        }
        if(null == capsRequested) {
            throw new IllegalArgumentException("Null requested caps");
        }
        this.screen = screen;
        this.capabilitiesChosen = capsChosen;
        this.capabilitiesRequested = capsRequested;
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
    final public AbstractGraphicsScreen getScreen() {
        return screen;
    }

    @Override
    final public CapabilitiesImmutable getChosenCapabilities() {
        return capabilitiesChosen;
    }

    @Override
    final public CapabilitiesImmutable getRequestedCapabilities() {
        return capabilitiesRequested;
    }

    @Override
    public AbstractGraphicsConfiguration getNativeGraphicsConfiguration() {
        return this;
    }

    @Override
    final public int getVisualID(final VIDType type) throws NativeWindowException {
        return capabilitiesChosen.getVisualID(type);
    }

    /**
     * Set the capabilities to a new value.
     *
     * <p>
     * The use case for setting the Capabilities at a later time is
     * a change or re-validation of capabilities.
     * </p>
     * @see com.jogamp.nativewindow.GraphicsConfigurationFactory#chooseGraphicsConfiguration(Capabilities, CapabilitiesChooser, AbstractGraphicsScreen)
     */
    protected void setChosenCapabilities(final CapabilitiesImmutable capsChosen) {
        this.capabilitiesChosen = capsChosen;
    }

    /**
     * Set a new screen.
     *
     * <p>
     * the use case for setting a new screen at a later time is
     * a change of the graphics device in a multi-screen environment.
     * </p>
     */
    protected void setScreen(final AbstractGraphicsScreen screen) {
        this.screen = screen;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"[" + screen +
                                       ",\n\tchosen    " + capabilitiesChosen+
                                       ",\n\trequested " + capabilitiesRequested+
                                       "]";
    }

    public static String toHexString(final int val) {
        return "0x"+Integer.toHexString(val);
    }

    public static String toHexString(final long val) {
        return "0x"+Long.toHexString(val);
    }
}
