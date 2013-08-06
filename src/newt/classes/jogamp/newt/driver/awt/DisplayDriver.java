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
 * 
 */

package jogamp.newt.driver.awt;

import javax.media.nativewindow.AbstractGraphicsDevice;

import com.jogamp.nativewindow.awt.AWTGraphicsDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.util.EDTUtil;

import jogamp.newt.DisplayImpl;

public class DisplayDriver extends DisplayImpl {
    public DisplayDriver() {
    }

    protected void createNativeImpl() {
        aDevice = AWTGraphicsDevice.createDefault();
    }

    protected void setAWTGraphicsDevice(AWTGraphicsDevice d) {
        aDevice = d;
    }

    protected EDTUtil createEDTUtil() {
        final EDTUtil def;
        if(NewtFactory.useEDT()) {
            def = new AWTEDTUtil(Thread.currentThread().getThreadGroup(), "AWTDisplay-"+getFQName(), dispatchMessagesRunnable);            
            if(DEBUG) {
                System.err.println("Display.createNative("+getFQName()+") Create EDTUtil: "+def.getClass().getName());
            }
        } else {
            def = null;
        }
        return def;
    }

    protected void closeNativeImpl(AbstractGraphicsDevice aDevice) { 
        aDevice.close();
    }

    protected void dispatchMessagesNative() { /* nop */ }
}

