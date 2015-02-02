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

package com.jogamp.opengl.awt;

import com.jogamp.opengl.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;

/** Factors out the listener manipulation for the events supported by
    all of the {@link GLDrawable} implementations. Provided to reduce
    clutter in the documentation for GLDrawable. */

public interface ComponentEvents {
  public void addComponentListener(ComponentListener l);
  public void removeComponentListener(ComponentListener l);
  public void addFocusListener(FocusListener l);
  public void removeFocusListener(FocusListener l);
  public void addHierarchyBoundsListener(HierarchyBoundsListener l);
  public void removeHierarchyBoundsListener(HierarchyBoundsListener l);
  public void addHierarchyListener(HierarchyListener l);
  public void removeHierarchyListener(HierarchyListener l);
  public void addInputMethodListener(InputMethodListener l);
  public void removeInputMethodListener(InputMethodListener l);
  public void addKeyListener(KeyListener l);
  public void removeKeyListener(KeyListener l);
  public void addMouseListener(MouseListener l);
  public void removeMouseListener(MouseListener l);
  public void addMouseMotionListener(MouseMotionListener l);
  public void removeMouseMotionListener(MouseMotionListener l);
  public void addMouseWheelListener(MouseWheelListener l);
  public void removeMouseWheelListener(MouseWheelListener l);
  public void addPropertyChangeListener(PropertyChangeListener listener);
  public void removePropertyChangeListener(PropertyChangeListener listener);
  public void addPropertyChangeListener(String propertyName,
                                        PropertyChangeListener listener);
  public void removePropertyChangeListener(String propertyName,
                                           PropertyChangeListener listener);
}
