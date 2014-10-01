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

package javax.media.opengl;

/** A generic exception for OpenGL errors used throughout the binding
    as a substitute for {@link RuntimeException}. */
@SuppressWarnings("serial")
public class GLException extends RuntimeException {
  /** Constructs a GLException object. */
  public GLException() {
    super();
  }

  /** Constructs a GLException object with the specified detail
      message. */
  public GLException(final String message) {
    super(message);
  }

  /** Constructs a GLException object with the specified detail
      message and root cause. */
  public GLException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /** Constructs a GLException object with the specified root
      cause. */
  public GLException(final Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a GLException object with the specified root
   * cause with a decorating message including the current thread name.
   * @since 2.2
   */
  public static GLException newGLException(final Throwable t) {
      return new GLException("Caught "+t.getClass().getSimpleName()+": "+t.getMessage()+" on thread "+Thread.currentThread().getName(), t);
  }

  /**
   * Dumps a Throwable in a decorating message including the current thread name, and stack trace.
   * @since 2.2
   */
  public static void dumpThrowable(final String additionalDescr, final Throwable t) {
      System.err.println("Caught "+additionalDescr+" "+t.getClass().getSimpleName()+": "+t.getMessage()+" on thread "+Thread.currentThread().getName());
      t.printStackTrace();
  }
}
