/*
 * Copyright (c) 2003-2009 Sun Microsystems, Inc. All Rights Reserved.
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

package javax.media.opengl;

import javax.media.nativewindow.NativeWindowException;
import jogamp.opengl.Debug;

import java.security.AccessController;
import java.util.List;
import javax.media.nativewindow.CapabilitiesImmutable;

/** <P> The default implementation of the {@link
    GLCapabilitiesChooser} interface, which provides consistent visual
    selection behavior across platforms. The precise algorithm is
    deliberately left loosely specified. Some properties are: </P>

    <UL>

    <LI> As long as there is at least one available non-null
    GLCapabilities which matches the "stereo" option, will return a
    valid index.

    <LI> Attempts to match as closely as possible the given
    GLCapabilities, but will select one with fewer capabilities (i.e.,
    lower color depth) if necessary.

    <LI> Prefers hardware-accelerated visuals to
    non-hardware-accelerated.

    <LI> If there is no exact match, prefers a more-capable visual to
    a less-capable one.

    <LI> If there is more than one exact match, chooses an arbitrary
    one.

    <LI> May select the opposite of a double- or single-buffered
    visual (based on the user's request) in dire situations.

    <LI> Color depth (including alpha) mismatches are weighted higher
    than depth buffer mismatches, which are in turn weighted higher
    than accumulation buffer (including alpha) and stencil buffer
    depth mismatches.

    <LI> If a valid windowSystemRecommendedChoice parameter is
    supplied, chooses that instead of using the cross-platform code.

    </UL>
*/

public class DefaultGLCapabilitiesChooser implements GLCapabilitiesChooser {
  private static final boolean DEBUG = Debug.isPropertyDefined("jogl.debug.CapabilitiesChooser", true, AccessController.getContext());

  public int chooseCapabilities(final CapabilitiesImmutable desired,
                                final List /*<CapabilitiesImmutable>*/ available,
                                final int windowSystemRecommendedChoice) {
    if ( null == desired ) {
      throw new NativeWindowException("Null desired capabilities");
    }
    if ( 0 == available.size() ) {
      throw new NativeWindowException("Empty available capabilities");
    }

    final GLCapabilitiesImmutable gldes = (GLCapabilitiesImmutable) desired;
    final int availnum = available.size();

    if (DEBUG) {
      System.err.println("Desired: " + gldes);
      System.err.println("Available: " + availnum);
      for (int i = 0; i < available.size(); i++) {
        System.err.println(i + ": " + available.get(i));
      }
      System.err.println("Window system's recommended choice: " + windowSystemRecommendedChoice);
    }

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
    int[] scores = new int[availnum];
    final int NO_SCORE = -9999999;
    final int DOUBLE_BUFFER_MISMATCH_PENALTY = 1000;
    final int OPAQUE_MISMATCH_PENALTY = 750;
    final int STENCIL_MISMATCH_PENALTY = 500;
    final int MULTISAMPLE_MISMATCH_PENALTY = 500;
    final int MULTISAMPLE_EXTENSION_MISMATCH_PENALTY = 250; // just a little drop, no scale
    // Pseudo attempt to keep equal rank penalties scale-equivalent
    // (e.g., stencil mismatch is 3 * accum because there are 3 accum
    // components)
    final int COLOR_MISMATCH_PENALTY_SCALE     = 36;
    final int DEPTH_MISMATCH_PENALTY_SCALE     = 6;
    final int ACCUM_MISMATCH_PENALTY_SCALE     = 1;
    final int STENCIL_MISMATCH_PENALTY_SCALE   = 3;
    final int MULTISAMPLE_MISMATCH_PENALTY_SCALE   = 3;
    
    for (int i = 0; i < scores.length; i++) {
      scores[i] = NO_SCORE;
    }
    final int gldes_samples = gldes.getSampleBuffers() ? gldes.getNumSamples() : 0;
    
    // Compute score for each
    for (int i = 0; i < availnum; i++) {
      GLCapabilitiesImmutable cur = (GLCapabilitiesImmutable) available.get(i);
      if (cur == null) {
        continue;
      }
      if (gldes.isOnscreen() != cur.isOnscreen()) {
        continue;
      }
      if (!gldes.isOnscreen() && gldes.isPBuffer() && !cur.isPBuffer()) {
        continue; // only skip if requested Offscreen && PBuffer, but no PBuffer available
      }
      if (gldes.getStereo() != cur.getStereo()) {
        continue;
      }
      final int cur_samples = 
              cur.getSampleBuffers() ? cur.getNumSamples() : 0;
      int score = 0;
              
      // Compute difference in color depth
      // (Note that this decides the direction of all other penalties)
      score += (COLOR_MISMATCH_PENALTY_SCALE *
                ((cur.getRedBits() + cur.getGreenBits() + cur.getBlueBits() + cur.getAlphaBits()) -
                 (gldes.getRedBits() + gldes.getGreenBits() + gldes.getBlueBits() + gldes.getAlphaBits())));
      // Compute difference in depth buffer depth
      score += (DEPTH_MISMATCH_PENALTY_SCALE * sign(score) * 
                Math.abs(cur.getDepthBits() - gldes.getDepthBits()));
      // Compute difference in accumulation buffer depth
      score += (ACCUM_MISMATCH_PENALTY_SCALE * sign(score) *
                Math.abs((cur.getAccumRedBits() + cur.getAccumGreenBits() + cur.getAccumBlueBits() + cur.getAccumAlphaBits()) -
                         (gldes.getAccumRedBits() + gldes.getAccumGreenBits() + gldes.getAccumBlueBits() + gldes.getAccumAlphaBits())));
      // Compute difference in stencil bits
      score += STENCIL_MISMATCH_PENALTY_SCALE * sign(score) * (cur.getStencilBits() - gldes.getStencilBits());
      // Compute difference in multisampling bits
      score += MULTISAMPLE_MISMATCH_PENALTY_SCALE * sign(score) * (cur_samples - gldes_samples);
      // double buffer
      if (cur.getDoubleBuffered() != gldes.getDoubleBuffered()) {
        score += sign(score) * DOUBLE_BUFFER_MISMATCH_PENALTY;
      }
      // opaque
      if (cur.isBackgroundOpaque() != gldes.isBackgroundOpaque()) {
        score += sign(score) * OPAQUE_MISMATCH_PENALTY;
      }
      if ((gldes.getStencilBits() > 0) && (cur.getStencilBits() == 0)) {
        score += sign(score) * STENCIL_MISMATCH_PENALTY;
      }
      if (gldes_samples > 0) {
          if (cur_samples == 0) {
            score += sign(score) * MULTISAMPLE_MISMATCH_PENALTY;
          }
          if (!gldes.getSampleExtension().equals(cur.getSampleExtension())) {
            score += sign(score) * MULTISAMPLE_EXTENSION_MISMATCH_PENALTY;
          }
      }
      scores[i] = score;
    }
    // Now prefer hardware-accelerated visuals by pushing scores of
    // non-hardware-accelerated visuals out
    boolean gotHW = false;
    int maxAbsoluteHWScore = 0;
    for (int i = 0; i < availnum; i++) {
      int score = scores[i];
      if (score == NO_SCORE) {
        continue;
      }
      GLCapabilitiesImmutable cur = (GLCapabilitiesImmutable) available.get(i);
      if (cur.getHardwareAccelerated()) {
        int absScore = Math.abs(score);
        if (!gotHW ||
            (absScore > maxAbsoluteHWScore)) {
          gotHW = true;
          maxAbsoluteHWScore = absScore;
        }
      }
    }
    if (gotHW) {
      for (int i = 0; i < availnum; i++) {
        int score = scores[i];
        if (score == NO_SCORE) {
          continue;
        }
        GLCapabilitiesImmutable cur = (GLCapabilitiesImmutable) available.get(i);
        if (!cur.getHardwareAccelerated()) {
          if (score <= 0) {
            score -= maxAbsoluteHWScore;
          } else if (score > 0) {
            score += maxAbsoluteHWScore;
          }
          scores[i] = score;
        }
      }
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
      int score = scores[i];
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
      throw new NativeWindowException("Unable to select one of the provided GLCapabilities");
    }
    if (DEBUG) {
      System.err.println("Chosen index: " + chosenIndex);
      System.err.println("Chosen capabilities:");
      System.err.println(available.get(chosenIndex));
    }

    return chosenIndex;
  }

  private static int sign(int score) {
    if (score < 0) {
      return -1;
    }
    return 1;
  }

}
