/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * Helper class to parse Uri's and programmatically add package names and properties to create an Uri or Intend.
 * <p>
 * The order of the Uri segments (any arguments) is preserved.
 * </p>  
 */
public class LauncherUtil {
    
   /** Default launch mode. */
   public static final String LAUNCH_ACTIVITY_NORMAL = "org.jogamp.launcher.action.LAUNCH_ACTIVITY_NORMAL";
   
   /** Transparent launch mode. Note: This seems to be required to achieve translucency, since setTheme(..) doesn't work. */
   public static final String LAUNCH_ACTIVITY_TRANSPARENT = "org.jogamp.launcher.action.LAUNCH_ACTIVITY_TRANSPARENT";
   
   /** FIXME: TODO */
   public static final String LAUNCH_MAIN = "org.jogamp.launcher.action.LAUNCH_MAIN";
   
   /** FIXME: TODO */
   public static final String LAUNCH_JUNIT = "org.jogamp.launcher.action.LAUNCH_JUNIT";
   
   /** The protocol <code>launch</code> */
   public static final String SCHEME = "launch";
   
   /** The host <code>jogamp.org</code> */
   public static final String HOST = "jogamp.org";
   
   static final String PKG = "pkg";
   
   public static abstract class BaseActivityLauncher extends Activity {
       final OrderedProperties props = new OrderedProperties();
       
       /** 
        * Returns the default {@link LauncherUtil#LAUNCH_ACTIVITY_NORMAL} action.
        * <p>
        * Should be overridden for other action, eg.  {@link LauncherUtil#LAUNCH_ACTIVITY_TRANSPARENT}.
        * </p>
        */
       public String getAction() { return LAUNCH_ACTIVITY_NORMAL; }
       
       /**
        * Returns the properties, which are being propagated to the target activity.
        * <p>
        * Maybe be used to set custom properties.
        * </p>
        */
       public final OrderedProperties getProperties() { return props; }
       
       /** Custom initialization hook which can be overriden to setup data, e.g. fill the properties retrieved by {@link #getProperties()}. */
       public void init() { }
       
       /** Returns true if this launcher activity shall end after starting the downstream activity. Defaults to <code>true</code>, override to change behavior. */
       public boolean finishAfterDelegate() { return true; }
       
       /** Must return the downstream Activity class name */
       public abstract String getActivityName();
       
       /** Must return a list of required packages, at least one. */
       public abstract List<String> getPackages();

       @Override
       public void onCreate(Bundle savedInstanceState) {
           super.onCreate(savedInstanceState);
           
           init();
           
           final DataSet data = new DataSet();
           data.setActivityName(getActivityName());
           data.addAllPackages(getPackages());
           data.addAllProperties(props);
           
           final Intent intent = LauncherUtil.getIntent(getAction(), data);
           Log.d(getClass().getSimpleName(), "Launching Activity: "+intent);
           startActivity (intent);
           
           if(finishAfterDelegate()) {
               finish(); // done
           }
       }        
   }
   
   public static class OrderedProperties {
       HashMap<String, String> map = new HashMap<String, String>();
       ArrayList<String> keyList = new ArrayList<String>(); 
              
       public final void setProperty(String key, String value) { 
           if(key.equals(PKG)) {
               throw new IllegalArgumentException("Illegal property key, '"+PKG+"' is reserved");
           }
           final String oval = map.put(key, value);
           if(null != oval) {
               map.put(key, oval); // restore
               throw new IllegalArgumentException("Property overwriting not allowed: "+key+": "+oval+" -> "+value);
           }
           keyList.add(key); // new key
       }
       
       public final void addAll(OrderedProperties props) {
           Iterator<String> argKeys = props.keyList.iterator();
           while(argKeys.hasNext()) {
                   final String key = argKeys.next();
                   setProperty(key, props.map.get(key));
           }           
       }
       
       public final void setSystemProperties() {
           Iterator<String> argKeys = keyList.iterator();
           while(argKeys.hasNext()) {
                   final String key = argKeys.next();
                   System.setProperty(key, map.get(key));
           }
       }
       public final void clearSystemProperties() {
           Iterator<String> argKeys = keyList.iterator();
           while(argKeys.hasNext()) {
               System.clearProperty(argKeys.next());
           }
       }
       
       public final String getProperty(String key) { return map.get(key); }
       public final Map<String, String> getProperties() { return map; }
           
       /** Returns the list of property keys in the order, as they were added. */
       public final List<String> getPropertyKeys() { return keyList; }       
   }
   
   public static class DataSet {
       static final char SLASH = '/';
       static final char QMARK = '?';
       static final char AMPER = '&';
       static final char ASSIG = '=';
       static final String COLSLASH2 = "://";
       static final String EMPTY = "";
       
       String activityName = null;
       ArrayList<String> packages = new ArrayList<String>();
       OrderedProperties properties = new OrderedProperties();
       
       public final void setActivityName(String name) { activityName = name; }
       public final String getActivityName() { return activityName; }
       
       public final void addPackage(String p) { 
           packages.add(p); 
       }   
       public final void addAllPackages(List<String> plist) { 
           packages.addAll(plist);
       }   
       public final List<String> getPackages()  { return packages; }
       
       public final void setProperty(String key, String value) {
           properties.setProperty(key, value);
       }
       public final void addAllProperties(OrderedProperties props) {
           properties.addAll(props);
       }
       public final void setSystemProperties() {
           properties.setSystemProperties();
       }   
       public final void clearSystemProperties() {
           properties.clearSystemProperties();
       }   
       public final String getProperty(String key) { return properties.getProperty(key); }
       public final OrderedProperties getProperties() { return properties; }
       public final List<String> getPropertyKeys() { return properties.getPropertyKeys(); }       
       
       public final Uri getUri() {
           StringBuilder sb = new StringBuilder();
           sb.append(SCHEME).append(COLSLASH2).append(HOST).append(SLASH).append(getActivityName());
           boolean needsSep = false;
           if(packages.size()>0) {
               sb.append(QMARK);
               for(int i=0; i<packages.size(); i++) {
                   if(needsSep) {
                       sb.append(AMPER);
                   }
                   sb.append(PKG).append(ASSIG).append(packages.get(i));
                   needsSep = true;
               }
           }
           Iterator<String> argKeys = properties.keyList.iterator();
           while(argKeys.hasNext()) {
                   if(needsSep) {
                       sb.append(AMPER);
                   }
                   final String key = argKeys.next();
                   sb.append(key).append(ASSIG).append(properties.map.get(key));
                   needsSep = true;
           }
           return Uri.parse(sb.toString());
       }
       
       public static final DataSet create(Uri uri) {
           if(!uri.getScheme().equals(SCHEME)) {
               return null;
           }
           if(!uri.getHost().equals(HOST)) {
               return null;
           }
           DataSet data = new DataSet();
           {
               String an =  uri.getPath();
               if(SLASH == an.charAt(0)) {
                   an = an.substring(1);
               }
               if(SLASH == an.charAt(an.length()-1)) {
                   an = an.substring(0, an.length()-1);
               }
               data.setActivityName(an);
           }
           
           final String q = uri.getQuery();
           final int q_l = null != q ? q.length() : -1;
           int q_e = -1;
           while(q_e < q_l) {
               int q_b = q_e + 1; // next term
               q_e = q.indexOf(AMPER, q_b);
               if(0 == q_e) {
                   // single seperator
                   continue; 
               }
               if(0 > q_e) {
                   // end
                   q_e = q_l;
               }
               // n-part
               final String part = q.substring(q_b, q_e);
               final int assignment = part.indexOf(ASSIG);
               if(0 < assignment) {
                   // assignment
                   final String k = part.substring(0, assignment);
                   final String v = part.substring(assignment+1);
                   if(k.equals(PKG)) {
                       if(v.length()==0) {
                           throw new IllegalArgumentException("Empty package name: part <"+part+">, query <"+q+"> of "+uri);
                       }
                       data.addPackage(v);
                   } else {
                       data.setProperty(k, v);
                   }
               } else {
                   // property key only
                   if(part.equals(PKG)) {
                       throw new IllegalArgumentException("Empty package name: part <"+part+">, query <"+q+"> of "+uri);
                   }
                   data.setProperty(part, EMPTY);
               }
           }
           data.validate();
           return data;
       }
       
       public final void validate() {
           if(null == activityName) {
               throw new RuntimeException("Activity is not NULL");
           }
       }
   }
   
   public final static Intent getIntent(String action, DataSet data) {
       data.validate();
       return new Intent(action, data.getUri());
   }
   
   public static void main(String[] args) {
       if(args.length==0) {
           args = new String[] {
               SCHEME+"://"+HOST+"/com.jogamp.TestActivity?"+PKG+"=jogamp.pack1&"+PKG+"=javax.pack2&"+PKG+"=com.jogamp.pack3&jogamp.common.debug=true&com.jogamp.test=false",   
               SCHEME+"://"+HOST+"/com.jogamp.TestActivity?"+PKG+"=jogamp.pack1&jogamp.common.debug=true&com.jogamp.test=false",   
               SCHEME+"://"+HOST+"/com.jogamp.TestActivity?"+PKG+"=jogamp.pack1"   
           };
       }
       for(int i=0; i<args.length; i++) {
           String uri_s = args[i];
           Uri uri0 = Uri.parse(uri_s);
           DataSet data = DataSet.create(uri0);
           if(null == data) {
               System.err.println("Error: NULL JogAmpLauncherUtil: <"+uri_s+"> -> "+uri0+" -> NULL");
           }
           Uri uri1 = data.getUri();
           if(!uri0.equals(uri1)) {
               System.err.println("Error: Not equal: <"+uri_s+"> -> "+uri0+" -> "+uri1);
           }
       }
   }
   
}
