/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.newt.event;

public class TraceMouseAdapter implements MouseListener {

 MouseListener downstream;

 public TraceMouseAdapter() {
    this.downstream = null;
 }

 public TraceMouseAdapter(final MouseListener downstream) {
    this.downstream = downstream;
 }

 @Override
 public void mouseClicked(final MouseEvent e) {
    System.err.println(e);
    if(null!=downstream) { downstream.mouseClicked(e); }
 }
 @Override
 public void mouseEntered(final MouseEvent e) {
    System.err.println(e);
    if(null!=downstream) { downstream.mouseEntered(e); }
 }
 @Override
 public void mouseExited(final MouseEvent e) {
    System.err.println(e);
    if(null!=downstream) { downstream.mouseExited(e); }
 }
 @Override
 public void mousePressed(final MouseEvent e) {
    System.err.println(e);
    if(null!=downstream) { downstream.mousePressed(e); }
 }
 @Override
 public void mouseReleased(final MouseEvent e) {
    System.err.println(e);
    if(null!=downstream) { downstream.mouseReleased(e); }
 }
 @Override
 public void mouseMoved(final MouseEvent e) {
    System.err.println(e);
    if(null!=downstream) { downstream.mouseMoved(e); }
 }
 @Override
 public void mouseDragged(final MouseEvent e) {
    System.err.println(e);
    if(null!=downstream) { downstream.mouseDragged(e); }
 }
 @Override
 public void mouseWheelMoved(final MouseEvent e) {
    System.err.println(e);
    if(null!=downstream) { downstream.mouseWheelMoved(e); }
 }
}

