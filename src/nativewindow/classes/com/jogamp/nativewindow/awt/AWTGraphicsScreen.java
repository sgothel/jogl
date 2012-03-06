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

package javax.media.nativewindow.awt;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import javax.media.nativewindow.*;

/** A wrapper for an AWT GraphicsDevice (screen) allowing it to be
    handled in a toolkit-independent manner. */

public class AWTGraphicsScreen extends DefaultGraphicsScreen implements Cloneable {

  public AWTGraphicsScreen(AWTGraphicsDevice device) {
    super(device, findScreenIndex(device.getGraphicsDevice()));
  }

  public static GraphicsDevice getScreenDevice(int index) {
    if(index<0) return null;
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] gs = ge.getScreenDevices();
    if(index<gs.length) {
        return gs[index];
    }
    return null;
  }

  public static int findScreenIndex(GraphicsDevice awtDevice) {
    if(null==awtDevice) return -1;
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] gs = ge.getScreenDevices();
    for (int j = 0; j < gs.length; j++) {
        if(gs[j] == awtDevice) return j;
    }
    return -1;
  }

  public static AbstractGraphicsScreen createScreenDevice(GraphicsDevice awtDevice, int unitID) {
    return new AWTGraphicsScreen(new AWTGraphicsDevice(awtDevice, unitID));
  }

  public static AbstractGraphicsScreen createScreenDevice(int index, int unitID) {
    return createScreenDevice(getScreenDevice(index), unitID);
  }

  public static AbstractGraphicsScreen createDefault() {
    return new AWTGraphicsScreen(AWTGraphicsDevice.createDefault());
  }

  public Object clone() {
      return super.clone();
  }
}

