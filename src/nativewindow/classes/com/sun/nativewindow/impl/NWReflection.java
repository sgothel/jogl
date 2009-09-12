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

package com.sun.nativewindow.impl;

import java.lang.reflect.*;
import javax.media.nativewindow.*;

public final class NWReflection {
  public static final boolean DEBUG = Debug.debug("NWReflection");

  public static final boolean isClassAvailable(String clazzName) {
    try {
        Class clazz = Class.forName(clazzName, false, NWReflection.class.getClassLoader());
        return null!=clazz;
    } catch (Throwable e) { }
    return false;
  }

  public static final Class getClass(String clazzName, boolean initialize) {
    try {
        return Class.forName(clazzName, initialize, NWReflection.class.getClassLoader());
    } catch (Throwable e) { }
    return null;
  }

  public static final Constructor getConstructor(String clazzName, Class[] cstrArgTypes) {
    Class factoryClass = null;
    Constructor factory = null;

    try {
        factoryClass = getClass(clazzName, true);
        if (factoryClass == null) {
          throw new NativeWindowException(clazzName + " not available");
        }
        return getConstructor(factoryClass, cstrArgTypes);
    } catch (Throwable e) { 
      if (DEBUG) {
          e.printStackTrace();
      }
      throw new NativeWindowException(e);
    }
  }

  public static final Constructor getConstructor(Class clazz, Class[] cstrArgTypes) {
    Constructor factory = null;

    try {
        try {
            factory = clazz.getDeclaredConstructor( cstrArgTypes );
        } catch(NoSuchMethodException nsme) {
          throw new NativeWindowException("Constructor: '" + clazz + "("+cstrArgTypes+")' not found");
        }
        return factory;
    } catch (Throwable e) { 
      if (DEBUG) {
          e.printStackTrace();
      }
      throw new NativeWindowException(e);
    }
  }

  public static final Constructor getConstructor(String clazzName) {
    return getConstructor(clazzName, new Class[0]);
  }

  public static final Object createInstance(Class clazz, Class[] cstrArgTypes, Object[] cstrArgs) {
    Constructor factory = null;

    try {
        factory = getConstructor(clazz, cstrArgTypes);
        return factory.newInstance( cstrArgs ) ;
    } catch (Exception e) {
      throw new NativeWindowException(e);
    }
  }

  public static final Object createInstance(Class clazz, Object[] cstrArgs) {
    Class[] cstrArgTypes = new Class[cstrArgs.length];
    for(int i=0; i<cstrArgs.length; i++) {
        cstrArgTypes[i] = cstrArgs[i].getClass();
    }
    return createInstance(clazz, cstrArgTypes, cstrArgs);
  }

  public static final Object createInstance(String clazzName, Class[] cstrArgTypes, Object[] cstrArgs) {
    Constructor factory = null;

    try {
        factory = getConstructor(clazzName, cstrArgTypes);
        return factory.newInstance( cstrArgs ) ;
    } catch (Exception e) {
      throw new NativeWindowException(e);
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
    return instanceOf(obj.getClass(), clazzName);
  }
  public static final boolean instanceOf(Class clazz, String clazzName) {
    do {
        if(clazz.getName().equals(clazzName)) {
            return true;
        }
        clazz = clazz.getSuperclass();
    } while (clazz!=null);
    return false;
  }

  public static final boolean implementationOf(Object obj, String faceName) {
    return implementationOf(obj.getClass(), faceName);
  }
  public static final boolean implementationOf(Class clazz, String faceName) {
    do {
        Class[] clazzes = clazz.getInterfaces();
        for(int i=clazzes.length-1; i>=0; i--) {
            Class face = clazzes[i];
            if(face.getName().equals(faceName)) {
                return true;
            }
        }
        clazz = clazz.getSuperclass();
    } while (clazz!=null);
    return false;
  }

  public static boolean isAWTComponent(Object target) {
      return instanceOf(target, "java.awt.Component");
  }

  public static boolean isAWTComponent(Class clazz) {
      return instanceOf(clazz, "java.awt.Component");
  }

}

