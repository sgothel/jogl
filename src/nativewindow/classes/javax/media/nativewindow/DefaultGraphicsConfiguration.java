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

package javax.media.nativewindow;

public class DefaultGraphicsConfiguration implements Cloneable, AbstractGraphicsConfiguration {
    private AbstractGraphicsScreen screen;
    protected CapabilitiesImmutable capabilitiesChosen;
    protected CapabilitiesImmutable capabilitiesRequested;

    public DefaultGraphicsConfiguration(AbstractGraphicsScreen screen, 
                                        CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested) {
        if(null == screen) {
            throw new NativeWindowException("Null screen");
        }
        if(null == capsChosen) {
            throw new NativeWindowException("Null chosen caps");
        }
        if(null == capsRequested) {
            throw new NativeWindowException("Null requested caps");
        }
        this.screen = screen;
        this.capabilitiesChosen = capsChosen;
        this.capabilitiesRequested = capsRequested;
    }

    @Override
    public Object clone() {
        try {
          return super.clone();
        } catch (CloneNotSupportedException e) {
          throw new NativeWindowException(e);
        }
    }

    public AbstractGraphicsScreen getScreen() {
        return screen;
    }

    public CapabilitiesImmutable getChosenCapabilities() {
        return capabilitiesChosen;
    }

    public CapabilitiesImmutable getRequestedCapabilities() {
        return capabilitiesRequested;
    }

    public AbstractGraphicsConfiguration getNativeGraphicsConfiguration() {
        return this;
    }

    /**
     * Set the capabilities to a new value.
     *
     * The use case for setting the Capabilities at a later time is
     * a change of the graphics device in a multi-screen environment.<br>
     *
     * @see javax.media.nativewindow.GraphicsConfigurationFactory#chooseGraphicsConfiguration(Capabilities, CapabilitiesChooser, AbstractGraphicsScreen)
     */
    protected void setChosenCapabilities(CapabilitiesImmutable capsChosen) {
        capabilitiesChosen = capsChosen;
    }

    /**
     * Set a new screen.
     *
     * the use case for setting a new screen at a later time is
     * a change of the graphics device in a multi-screen environment.<br>
     *
     * A copy of the passed object is being used.
     */
    protected void setScreen(DefaultGraphicsScreen screen) {
        this.screen = (AbstractGraphicsScreen) screen.clone();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"[" + screen +
                                       ",\n\tchosen    " + capabilitiesChosen+
                                       ",\n\trequested " + capabilitiesRequested+ 
                                       "]";
    }

    public static String toHexString(int val) {
        return "0x"+Integer.toHexString(val);
    }

    public static String toHexString(long val) {
        return "0x"+Long.toHexString(val);
    }
}
