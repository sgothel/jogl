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
    private Capabilities capabilities;

    public DefaultGraphicsConfiguration(AbstractGraphicsScreen screen, Capabilities caps) {
        this.screen = screen;
        this.capabilities = caps;
    }

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

    public Capabilities getCapabilities() {
        return (Capabilities)capabilities.clone();
    }

    public AbstractGraphicsConfiguration getNativeGraphicsConfiguration() {
        return this;
    }

    /**
     * Set the capabilities to a new value.
     * Despite the architecture's goal to gather this data upfront,
     * on some platforms we can gather the info only after the window creation..
     * However, if you can, please use {@link GraphicsConfigurationFactory GraphicsConfigurationFactory}.
     *
     * @see javax.media.nativewindow.GraphicsConfigurationFactory#chooseGraphicsConfiguration(Capabilities, CapabilitiesChooser, AbstractGraphicsScreen)
     */
    protected void setCapabilities(Capabilities caps) {
        capabilities = (Capabilities) caps.clone();
    }

    public String toString() {
        return getClass().toString()+"[" + screen +", "+ capabilities +"]";
    }
}
