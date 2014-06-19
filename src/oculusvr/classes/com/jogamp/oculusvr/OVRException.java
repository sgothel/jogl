/**
 * Copyright (C) 2014 JogAmp Community. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * BRIAN PAUL BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.jogamp.oculusvr;

/** A generic exception for OculusVR errors used throughout the binding
    as a substitute for {@link RuntimeException}. */
@SuppressWarnings("serial")
public class OVRException extends RuntimeException {
  /** Constructs an ALException object. */
  public OVRException() {
    super();
  }

  /** Constructs an ALException object with the specified detail
      message. */
  public OVRException(String message) {
    super(message);
  }

  /** Constructs an ALException object with the specified detail
      message and root cause. */
  public OVRException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Constructs an ALException object with the specified root
      cause. */
  public OVRException(Throwable cause) {
    super(cause);
  }
}
