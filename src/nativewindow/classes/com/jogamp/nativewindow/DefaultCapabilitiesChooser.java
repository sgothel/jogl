/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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

import java.util.List;

import com.jogamp.common.util.PropertyAccess;

import jogamp.nativewindow.Debug;

/** <P> The default implementation of the {@link
    CapabilitiesChooser} interface, which provides consistent visual
    selection behavior across platforms. The precise algorithm is
    deliberately left loosely specified. Some properties are: </P>

    <LI> Attempts to match as closely as possible the given
    Capabilities, but will select one with fewer capabilities (i.e.,
    lower color depth) if necessary.

    <LI> If there is no exact match, prefers a more-capable visual to
    a less-capable one.

    <LI> If there is more than one exact match, chooses an arbitrary
    one.

    <LI> If a valid windowSystemRecommendedChoice parameter is
    supplied, chooses that instead of using the cross-platform code.

    </UL>
*/

public class DefaultCapabilitiesChooser implements CapabilitiesChooser {
  private static final boolean DEBUG;

  static {
      Debug.initSingleton();
      DEBUG = PropertyAccess.isPropertyDefined("nativewindow.debug.CapabilitiesChooser", true);
  }

  private final static int NO_SCORE = -9999999;
  private final static int COLOR_MISMATCH_PENALTY_SCALE     = 36;

  @Override
  public int chooseCapabilities(final CapabilitiesImmutable desired,
                                final List<? extends CapabilitiesImmutable> available,
                                final int windowSystemRecommendedChoice) {
    if (DEBUG) {
      System.err.println("Desired: " + desired);
      for (int i = 0; i < available.size(); i++) {
        System.err.println("Available " + i + ": " + available.get(i));
      }
      System.err.println("Window system's recommended choice: " + windowSystemRecommendedChoice);
    }
    final int availnum = available.size();

    if (windowSystemRecommendedChoice >= 0 &&
        windowSystemRecommendedChoice < availnum &&
        null != available.get(windowSystemRecommendedChoice)) {
      if (DEBUG) {
        System.err.println("Choosing window system's recommended choice of " + windowSystemRecommendedChoice);
        System.err.println(available.get(windowSystemRecommendedChoice));
      }
      return windowSystemRecommendedChoice;
    }

    // Create score array
    final int[] scores = new int[availnum];
    for (int i = 0; i < availnum; i++) {
      scores[i] = NO_SCORE;
    }
    // Compute score for each
    for (int i = 0; i < availnum; i++) {
      final CapabilitiesImmutable cur = available.get(i);
      if (cur == null) {
        continue;
      }
      if (desired.isOnscreen() && !cur.isOnscreen()) {
        continue; // requested onscreen, but n/a
      }

      int score = 0;
      // Compute difference in color depth
      score += (COLOR_MISMATCH_PENALTY_SCALE *
                ((cur.getRedBits() + cur.getGreenBits() + cur.getBlueBits() + cur.getAlphaBits()) -
                 (desired.getRedBits() + desired.getGreenBits() + desired.getBlueBits() + desired.getAlphaBits())));
      scores[i] = score;
    }

    if (DEBUG) {
      System.err.print("Scores: [");
      for (int i = 0; i < availnum; i++) {
        if (i > 0) {
          System.err.print(",");
        }
        System.err.print(" " + scores[i]);
      }
      System.err.println(" ]");
    }

    // Ready to select. Choose score closest to 0.
    int scoreClosestToZero = NO_SCORE;
    int chosenIndex = -1;
    for (int i = 0; i < availnum; i++) {
      final int score = scores[i];
      if (score == NO_SCORE) {
        continue;
      }
      // Don't substitute a positive score for a smaller negative score
      if ((scoreClosestToZero == NO_SCORE) ||
          (Math.abs(score) < Math.abs(scoreClosestToZero) &&
       ((sign(scoreClosestToZero) < 0) || (sign(score) > 0)))) {
        scoreClosestToZero = score;
        chosenIndex = i;
      }
    }
    if (chosenIndex < 0) {
      throw new NativeWindowException("Unable to select one of the provided Capabilities");
    }
    if (DEBUG) {
      System.err.println("Chosen index: " + chosenIndex);
      System.err.println("Chosen capabilities:");
      System.err.println(available.get(chosenIndex));
    }

    return chosenIndex;
  }

  private static int sign(final int score) {
    if (score < 0) {
      return -1;
    }
    return 1;
  }
}
