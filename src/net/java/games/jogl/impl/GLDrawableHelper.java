/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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

package net.java.games.jogl.impl;

import java.util.*;
import net.java.games.jogl.*;

/** Encapsulates the implementation of most of the GLDrawable's
    methods to be able to share it between GLCanvas and GLJPanel. */

public class GLDrawableHelper {
  private List listeners = new ArrayList();
  private GL gl;
  // FIXME
  //  private GLU glu;

  public GLDrawableHelper() {
  }

  public void addGLEventListener(GLEventListener listener) {
    listeners.add(listener);
  }
  
  public void removeGLEventListener(GLEventListener listener) {
    listeners.remove(listener);
  }

  public void init(GLDrawable drawable) {
    // Note that we don't use iterator() since listeners may
    // add/remove other listeners during initialization. We don't
    // guarantee that all listeners will be evaluated if
    // removeGLEventListener is called.
    for (int i = 0; i < listeners.size(); i++) {
      ((GLEventListener) listeners.get(i)).init(drawable);
    }
  }

  public void display(GLDrawable drawable) {
    for (int i = 0; i < listeners.size(); i++) {
      ((GLEventListener) listeners.get(i)).display(drawable);
    }
  }

  public void reshape(GLDrawable drawable,
                      int x, int y, int width, int height) {
    for (int i = 0; i < listeners.size(); i++) {
      ((GLEventListener) listeners.get(i)).reshape(drawable, x, y, width, height);
    }
  }
}
