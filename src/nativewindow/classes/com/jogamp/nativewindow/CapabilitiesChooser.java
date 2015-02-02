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

package com.jogamp.nativewindow;

import java.util.List;

/** Provides a mechanism by which applications can customize the
    window type selection for a given {@link Capabilities}.
    Developers can implement this interface and pass an instance into
    the method {@link GraphicsConfigurationFactory#chooseGraphicsConfiguration}; the chooser
    will be called at window creation time. */

public interface CapabilitiesChooser {
  /** Chooses the index (0..available.length - 1) of the {@link
      Capabilities} most closely matching the desired one from the
      list of all supported. Some of the entries in the
      <code>available</code> array may be null; the chooser must
      ignore these. The <em>windowSystemRecommendedChoice</em>
      parameter may be provided to the chooser by the underlying
      window system; if this index is valid, it is recommended, but
      not necessarily required, that the chooser select that entry.

      <P> <em>Note:</em> this method is called automatically by the
      {@link GraphicsConfigurationFactory#chooseGraphicsConfiguration} method
      when an instance of this class is passed in to it.
      It should generally not be
      invoked by users directly, unless it is desired to delegate the
      choice to some other CapabilitiesChooser object.
  */
  public int chooseCapabilities(CapabilitiesImmutable desired,
                                List<? extends CapabilitiesImmutable> available,
                                int windowSystemRecommendedChoice);
}
