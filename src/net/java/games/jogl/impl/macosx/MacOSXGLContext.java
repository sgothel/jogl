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

package net.java.games.jogl.impl.macosx;

import java.awt.Component;
import java.util.*;
import net.java.games.gluegen.opengl.*; // for PROCADDRESS_VAR_PREFIX
import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public abstract class MacOSXGLContext extends GLContext
{	
  private static JAWT jawt;
  protected long nsContext; // NSOpenGLContext
	
  public MacOSXGLContext(Component component, GLCapabilities capabilities, GLCapabilitiesChooser chooser)
  {
    super(component, capabilities, chooser);
  }
	
  protected String mapToRealGLFunctionName(String glFunctionName)
  {
    return glFunctionName;
  }
	
  protected String mapToRealGLExtensionName(String glFunctionName)
  {
    return glFunctionName;
  }
	
  protected boolean isFunctionAvailable(String glFunctionName)
  {
    return super.isFunctionAvailable(glFunctionName);
  }
  
  protected abstract boolean isOffscreen();
	
  public abstract int getOffscreenContextBufferedImageType();

  public abstract int getOffscreenContextReadBuffer();

  public abstract boolean offscreenImageNeedsVerticalFlip();

  /**
   * Creates and initializes an appropriate OpenGl nsContext. Should only be
   * called by {@link makeCurrent(Runnable)}.
   */
  protected abstract void create();
	
  protected abstract boolean makeCurrent(Runnable initAction) throws GLException;
	
  protected abstract void free() throws GLException;
	
  protected abstract void swapBuffers() throws GLException;
	
	
  protected void resetGLFunctionAvailability()
  {
    super.resetGLFunctionAvailability();
    resetGLProcAddressTable();
  }
	
  protected void resetGLProcAddressTable()
  {    
    if (DEBUG) {
      System.err.println("!!! Initializing OpenGL extension address table");
    }

    net.java.games.jogl.impl.ProcAddressTable table = getGLProcAddressTable();
    
    // if GL is no longer an interface, we'll have to re-implement the code
    // below so it only iterates through gl methods (a non-interface might
    // have constructors, custom methods, etc). For now we assume all methods
    // will be gl methods.
    GL gl = getGL();

    Class tableClass = table.getClass();
    
    java.lang.reflect.Field[] fields = tableClass.getDeclaredFields();
    
    for (int i = 0; i < fields.length; ++i) {
      String addressFieldName = fields[i].getName();
      if (!addressFieldName.startsWith(GLEmitter.PROCADDRESS_VAR_PREFIX))
      {
        // not a proc address variable
        continue;
      }
      int startOfMethodName = GLEmitter.PROCADDRESS_VAR_PREFIX.length();
      String glFuncName = addressFieldName.substring(startOfMethodName);
      try
      {
        java.lang.reflect.Field addressField = tableClass.getDeclaredField(addressFieldName);
        assert(addressField.getType() == Long.TYPE);
        // get the current value of the proc address variable in the table object
        long oldProcAddress = addressField.getLong(table); 
        long newProcAddress = CGL.getProcAddress(glFuncName);
        /*
        System.err.println(
          "!!!   Address=" + (newProcAddress == 0 
                        ? "<NULL>    "
                        : ("0x" +
                           Long.toHexString(newProcAddress))) +
          "\tGL func: " + glFuncName);
        */
        // set the current value of the proc address variable in the table object
        addressField.setLong(gl, newProcAddress); 
      } catch (Exception e) {
        throw new GLException(
          "Cannot get GL proc address for method \"" +
          glFuncName + "\": Couldn't get value of field \"" + addressFieldName +
          "\" in class " + tableClass.getName(), e);
      }
    }

  }
	
  public net.java.games.jogl.impl.ProcAddressTable getGLProcAddressTable()
  {
    if (glProcAddressTable == null) {
      // FIXME: cache ProcAddressTables by capability bits so we can
      // share them among contexts with the same capabilities
      glProcAddressTable =
        new net.java.games.jogl.impl.ProcAddressTable();
    }          
    return glProcAddressTable;
  }
	
  public String getPlatformExtensionsString()
  {
    return "";
  }
	
  //----------------------------------------------------------------------
  // Internals only below this point
  //
	
  // Table that holds the addresses of the native C-language entry points for
  // OpenGL functions.
  private net.java.games.jogl.impl.ProcAddressTable glProcAddressTable;
	
  protected JAWT getJAWT()
  {
    if (jawt == null)
      {
	JAWT j = new JAWT();
	j.version(JAWTFactory.JAWT_VERSION_1_4);
	if (!JAWTFactory.JAWT_GetAWT(j))
	  {
	    throw new RuntimeException("Unable to initialize JAWT");
	  }
	jawt = j;
      }
    return jawt;
  }
}
