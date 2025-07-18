/**
 * Copyright 2012-2025 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.demos.androidfat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OrderedProperties {
   HashMap<String, String> map = new HashMap<String, String>();
   ArrayList<String> keyList = new ArrayList<String>();

   public final void setProperty(final String key, final String value) {
       final String oval = map.put(key, value);
       if(null != oval) {
           map.put(key, oval); // restore
           throw new IllegalArgumentException("Property overwriting not allowed: "+key+": "+oval+" -> "+value);
       }
       keyList.add(key); // new key
   }

   public final void addAll(final OrderedProperties props) {
       final Iterator<String> argKeys = props.keyList.iterator();
       while(argKeys.hasNext()) {
               final String key = argKeys.next();
               setProperty(key, props.map.get(key));
       }
   }

   public final void setSystemProperties() {
       final Iterator<String> argKeys = keyList.iterator();
       while(argKeys.hasNext()) {
               final String key = argKeys.next();
               System.setProperty(key, map.get(key));
       }
   }
   public final void clearSystemProperties() {
       final Iterator<String> argKeys = keyList.iterator();
       while(argKeys.hasNext()) {
           System.clearProperty(argKeys.next());
       }
   }

   public final String getProperty(final String key) { return map.get(key); }
   public final Map<String, String> getProperties() { return map; }

   /** Returns the list of property keys in the order, as they were added. */
   public final List<String> getPropertyKeys() { return keyList; }

}
