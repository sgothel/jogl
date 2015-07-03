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

   static final String SYS_PKG = "sys";

   static final String USR_PKG = "pkg";

   static final String ARG = "arg";

   public static abstract class BaseActivityLauncher extends Activity {
       final OrderedProperties props = new OrderedProperties();
       final ArrayList<String> args = new ArrayList<String>();
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

       /**
        * Returns the commandline arguments, which are being propagated to the target activity.
        * <p>
        * Maybe be used to set custom commandline arguments.
        * </p>
        */
       public final ArrayList<String> getArguments() { return args; }

       /** Custom initialization hook which can be overriden to setup data, e.g. fill the properties retrieved by {@link #getProperties()}. */
       public void init() { }

       /** Returns true if this launcher activity shall end after starting the downstream activity. Defaults to <code>true</code>, override to change behavior. */
       public boolean finishAfterDelegate() { return true; }

       /** Must return the downstream Activity class name */
       public abstract String getActivityName();

       /** Must return a list of required user packages, at least one containing the activity. */
       public abstract List<String> getUsrPackages();

       /** Return a list of required system packages w/ native libraries, may return null or a zero sized list. */
       public abstract List<String> getSysPackages();

       @Override
       public void onCreate(final Bundle savedInstanceState) {
           super.onCreate(savedInstanceState);

           init();

           final DataSet data = new DataSet();
           data.setActivityName(getActivityName());
           data.addAllSysPackages(getSysPackages());
           data.addAllUsrPackages(getUsrPackages());
           data.addAllProperties(props);
           data.addAllArguments(args);

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

       public final void setProperty(final String key, final String value) {
           if(key.equals(SYS_PKG)) {
               throw new IllegalArgumentException("Illegal property key, '"+SYS_PKG+"' is reserved");
           }
           if(key.equals(USR_PKG)) {
               throw new IllegalArgumentException("Illegal property key, '"+USR_PKG+"' is reserved");
           }
           if(key.equals(ARG)) {
               throw new IllegalArgumentException("Illegal property key, '"+ARG+"' is reserved");
           }
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

   /**
    * Data set to transfer from and to launch URI consisting out of:
    * <ul>
    *   <li>system packages w/ native libraries used on Android, which may use a cached ClassLoader, see {@link DataSet#getSysPackages()}.</li>
    *   <li>user packages w/o native libraries used on Android, which do not use a cached ClassLoader, see {@link DataSet#getUsrPackages()}.</li>
    *   <li>activity name, used to launch an Android activity, see {@link DataSet#getActivityName()}.</li>
    *   <li>properties, which will be added to the system properties, see {@link DataSet#getProperties()}.</li>
    *   <li>arguments, used to launch a class main-entry, see {@link DataSet#getArguments()}.</li>
    * </ul>
    * {@link DataSet#getUri()} returns a URI representation of all components.
    */
   public static class DataSet {
       static final char SLASH = '/';
       static final char QMARK = '?';
       static final char AMPER = '&';
       static final char ASSIG = '=';
       static final String COLSLASH2 = "://";
       static final String EMPTY = "";

       String activityName = null;
       ArrayList<String> sysPackages = new ArrayList<String>();
       ArrayList<String> usrPackages = new ArrayList<String>();
       OrderedProperties properties = new OrderedProperties();
       ArrayList<String> arguments = new ArrayList<String>();

       public final void setActivityName(final String name) { activityName = name; }
       public final String getActivityName() { return activityName; }

       public final void addSysPackage(final String p) {
           sysPackages.add(p);
       }
       public final void addAllSysPackages(final List<String> plist) {
           sysPackages.addAll(plist);
       }
       public final List<String> getSysPackages()  { return sysPackages; }

       public final void addUsrPackage(final String p) {
           usrPackages.add(p);
       }
       public final void addAllUsrPackages(final List<String> plist) {
           usrPackages.addAll(plist);
       }
       public final List<String> getUsrPackages()  { return usrPackages; }

       public final void setProperty(final String key, final String value) {
           properties.setProperty(key, value);
       }
       public final void addAllProperties(final OrderedProperties props) {
           properties.addAll(props);
       }
       public final void setSystemProperties() {
           properties.setSystemProperties();
       }
       public final void clearSystemProperties() {
           properties.clearSystemProperties();
       }
       public final String getProperty(final String key) { return properties.getProperty(key); }
       public final OrderedProperties getProperties() { return properties; }
       public final List<String> getPropertyKeys() { return properties.getPropertyKeys(); }

       public final void addArgument(final String arg) { arguments.add(arg); }
       public final void addAllArguments(final List<String> args) {
           arguments.addAll(args);
       }
       public final ArrayList<String> getArguments() { return arguments; }

       public final Uri getUri() {
           final StringBuilder sb = new StringBuilder();
           sb.append(SCHEME).append(COLSLASH2).append(HOST).append(SLASH).append(getActivityName());
           boolean needsQMark = true;
           boolean needsSep = false;
           if(sysPackages.size()>0) {
               if( needsQMark ) {
                   sb.append(QMARK);
                   needsQMark = false;
               }
               for(int i=0; i<sysPackages.size(); i++) {
                   if(needsSep) {
                       sb.append(AMPER);
                   }
                   sb.append(SYS_PKG).append(ASSIG).append(sysPackages.get(i));
                   needsSep = true;
               }
           }
           if(usrPackages.size()>0) {
               if( needsQMark ) {
                   sb.append(QMARK);
                   needsQMark = false;
               }
               for(int i=0; i<usrPackages.size(); i++) {
                   if(needsSep) {
                       sb.append(AMPER);
                   }
                   sb.append(USR_PKG).append(ASSIG).append(usrPackages.get(i));
                   needsSep = true;
               }
           }
           final Iterator<String> propKeys = properties.keyList.iterator();
           while(propKeys.hasNext()) {
               if( needsQMark ) {
                   sb.append(QMARK);
                   needsQMark = false;
               }
               if(needsSep) {
                   sb.append(AMPER);
               }
               final String key = propKeys.next();
               sb.append(key).append(ASSIG).append(properties.map.get(key));
               needsSep = true;
           }
           final Iterator<String> args = arguments.iterator();
           while(args.hasNext()) {
               if( needsQMark ) {
                   sb.append(QMARK);
                   needsQMark = false;
               }
               if(needsSep) {
                   sb.append(AMPER);
               }
               sb.append(ARG).append(ASSIG).append(args.next());
               needsSep = true;
           }
           return Uri.parse(sb.toString());
       }

       public static final DataSet create(final Uri uri) {
           if(!uri.getScheme().equals(SCHEME)) {
               return null;
           }
           if(!uri.getHost().equals(HOST)) {
               return null;
           }
           final DataSet data = new DataSet();
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
               final int q_b = q_e + 1; // next term
               q_e = q.indexOf(AMPER, q_b);
               if(0 == q_e) {
                   // single separator
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
                   if(k.equals(SYS_PKG)) {
                       if(v.length()==0) {
                           throw new IllegalArgumentException("Empty package name: part <"+part+">, query <"+q+"> of "+uri);
                       }
                       data.addSysPackage(v);
                   } else if(k.equals(USR_PKG)) {
                       if(v.length()==0) {
                           throw new IllegalArgumentException("Empty package name: part <"+part+">, query <"+q+"> of "+uri);
                       }
                       data.addUsrPackage(v);
                   } else if(k.equals(ARG)) {
                       if(v.length()==0) {
                           throw new IllegalArgumentException("Empty argument name: part <"+part+">, query <"+q+"> of "+uri);
                       }
                       data.addArgument(v);
                   } else {
                       data.setProperty(k, v);
                   }
               } else {
                   // property key only
                   if( part.equals(USR_PKG) || part.equals(ARG) ) {
                       throw new IllegalArgumentException("Reserved key <"+part+"> in query <"+q+"> of "+uri);
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

   public final static Intent getIntent(final String action, final DataSet data) {
       data.validate();
       return new Intent(action, data.getUri());
   }

   public static void main(String[] args) {
       if(args.length==0) {
           args = new String[] {
               SCHEME+"://"+HOST+"/com.jogamp.TestActivity?"+SYS_PKG+"=jogamp.pack1&"+SYS_PKG+"=javax.pack2&"+USR_PKG+"=com.jogamp.pack3&"+USR_PKG+"=com.jogamp.pack4&jogamp.common.debug=true&com.jogamp.test=false",
               SCHEME+"://"+HOST+"/com.jogamp.TestActivity?"+SYS_PKG+"=jogamp.pack1&jogamp.common.debug=true&com.jogamp.test=false",
               SCHEME+"://"+HOST+"/com.jogamp.TestActivity?"+USR_PKG+"=jogamp.pack1&jogamp.common.debug=true&com.jogamp.test=false",
               SCHEME+"://"+HOST+"/com.jogamp.TestActivity?"+USR_PKG+"=jogamp.pack1&"+USR_PKG+"=com.jogamp.pack2",
               SCHEME+"://"+HOST+"/com.jogamp.TestActivity?"+USR_PKG+"=jogamp.pack1&"+USR_PKG+"=javax.pack2&"+USR_PKG+"=com.jogamp.pack3&jogamp.common.debug=true&com.jogamp.test=false&"+ARG+"=arg1&"+ARG+"=arg2=arg2value&"+ARG+"=arg3",
               SCHEME+"://"+HOST+"/com.jogamp.TestActivity?"+USR_PKG+"=jogamp.pack1&jogamp.common.debug=true&com.jogamp.test=false&"+ARG+"=arg1&"+ARG+"=arg2=arg2value&"+ARG+"=arg3",
               SCHEME+"://"+HOST+"/com.jogamp.TestActivity?"+USR_PKG+"=jogamp.pack1&"+ARG+"=arg1&"+ARG+"=arg2=arg2value&"+ARG+"=arg3"
           };
       }
       int errors = 0;
       for(int i=0; i<args.length; i++) {
           final String uri_s = args[i];
           final Uri uri0 = Uri.parse(uri_s);
           final DataSet data = DataSet.create(uri0);
           if(null == data) {
               errors++;
               System.err.println("Error: NULL JogAmpLauncherUtil: <"+uri_s+"> -> "+uri0+" -> NULL");
           } else {
               final Uri uri1 = data.getUri();
               if(!uri0.equals(uri1)) {
                   errors++;
                   System.err.println("Error: Not equal: <"+uri_s+"> -> "+uri0+" -> "+uri1);
               } else {
                   System.err.println("OK: "+uri1);
               }
           }
       }
       System.err.println("LauncherUtil Self Test: Errors: "+errors);
   }

}
