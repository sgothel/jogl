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

public class DefaultGraphicsScreen implements Cloneable, AbstractGraphicsScreen {
    private final AbstractGraphicsDevice device;
    private final int idx;

    public DefaultGraphicsScreen(final AbstractGraphicsDevice device, final int idx) {
        this.device = device;
        this.idx = idx;
    }

    public static AbstractGraphicsScreen createDefault(final String type) {
        return new DefaultGraphicsScreen(new DefaultGraphicsDevice(type, DefaultGraphicsDevice.getDefaultDisplayConnection(type), AbstractGraphicsDevice.DEFAULT_UNIT), 0);
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
    public AbstractGraphicsDevice getDevice() {
        return device;
    }

    @Override
    public int getIndex() {
      return idx;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"["+device+", idx "+idx+"]";
    }
}
