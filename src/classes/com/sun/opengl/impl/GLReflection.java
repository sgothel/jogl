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
 */

package com.sun.opengl.impl;

import java.lang.reflect.*;
import javax.media.opengl.*;

public final class GLReflection {

  public static final Constructor getConstructor(String clazzName, Class[] cstrArgTypes) {
    Class factoryClass = null;
    Constructor factory = null;

    try {
        factoryClass = Class.forName(clazzName);
        if (factoryClass == null) {
          throw new GLException(clazzName + " not available");
        }
        try {
            factory = factoryClass.getDeclaredConstructor( cstrArgTypes );
        } catch(NoSuchMethodException nsme) {
          throw new GLException("Constructor: '" + clazzName + "("+cstrArgTypes+")' not found");
        }
        return factory;
    } catch (Exception e) {
      throw new GLException(e);
    }
  }

  public static final Constructor getConstructor(String clazzName) {
    return getConstructor(clazzName, new Class[0]);
  }

  public static final Object createInstance(String clazzName, Class[] cstrArgTypes, Object[] cstrArgs) {
    Constructor factory = null;

    try {
        factory = getConstructor(clazzName, cstrArgTypes);
        return factory.newInstance( cstrArgs ) ;
    } catch (Exception e) {
      throw new GLException(e);
    }
  }

  public static final Object createInstance(String clazzName, Object[] cstrArgs) {
    Class[] cstrArgTypes = new Class[cstrArgs.length];
    for(int i=0; i<cstrArgs.length; i++) {
        cstrArgTypes[i] = cstrArgs[i].getClass();
    }
    return createInstance(clazzName, cstrArgTypes, cstrArgs);
  }

  public static final Object createInstance(String clazzName) {
    return createInstance(clazzName, new Class[0], null);
  }

  public static final boolean instanceOf(Object obj, String clazzName) {
    Class clazz = obj.getClass();
    do {
        if(clazz.getName().equals(clazzName)) {
            return true;
        }
        clazz = clazz.getSuperclass();
    } while (clazz!=null);
    return false;
  }

  public static final boolean implementationOf(Object obj, String faceName) {
    Class[] clazzes = obj.getClass().getInterfaces();
    for(int i=clazzes.length-1; i>=0; i--) {
        Class face = clazzes[i];
        if(face.getName().equals(faceName)) {
            return true;
        }
    }
    return false;
  }

}

