/* This java class is distributed under the BSD license.
 *
 * Copyright 2005 Lilian Chamontin.
 * contact lilian.chamontin at f r e e . f r
 */

/*
 * Portions Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.opengl.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Panel;
import java.applet.Applet;
import java.applet.AppletStub;
import java.applet.AppletContext;
import java.io.*;
import java.net.*;
import java.security.cert.*;
import java.util.*;
import java.util.jar.*;
import javax.swing.*;

import javax.media.opengl.*;


/** Basic JOGL installer for Applets. The key functionality this class
 *  supplies is the ability to deploy unsigned applets which use JOGL.
 *  It may also be used to deploy signed applets in which case
 *  multiple security dialogs will be displayed. <p>
 *
 *  On the server side the codebase must contain jogl.jar and all of
 *  the jogl-natives-*.jar files from the standard JOGL distribution.
 *  This is the location from which the JOGL library used by the
 *  applet is downloaded. The codebase additionally contains the jar
 *  file of the user's potentially untrusted applet. The jogl.jar and
 *  all jogl-natives jars must be signed by the same entity, which is
 *  typically Sun Microsystems, Inc.
 *
 * Sample applet code:
 * <pre>
 * &lt;applet code="com.sun.opengl.util.JOGLAppletLauncher"
 *      width=600
 *      height=400
 *      codebase="/lib"
 *      archive="jogl.jar,your_applet.jar"&gt;
 *   &lt;param name="subapplet.classname" VALUE="untrusted.JOGLApplet"&gt;
 *   &lt;param name="subapplet.displayname" VALUE="My JOGL Applet"&gt;
 *   &lt;param name="progressbar" value="true"&gt;
 *   &lt;param name="cache_archive" VALUE="jogl.jar,your_applet.jar"&gt;
 *   &lt;param name="cache_archive_ex" VALUE="jogl.jar;preload,your_applet.jar;preload"&gt;
 * &lt;/applet&gt;
 * </pre>
 * <p>
 * 
 * There are some limitations with this approach. It is not possible
 * to specify e.g. -Dsun.java2d.noddraw=true or
 * -Dsun.java2d.opengl=true for better control over the Java2D
 * pipeline as it is with Java Web Start. There appear to be issues
 * with multiple JOGL-based applets on the same web page, though
 * multiple instances of the same applet appear to work. The latter
 * may simply be a bug which needs to be fixed. <p>
 * 
 * The JOGL natives are cached in the user's home directory (the value
 * of the "user.home" system property in Java) under the directory
 * .jogl_ext. The Java Plug-In is responsible for performing all other
 * jar caching. If the JOGL installation is updated on the server, the
 * .jogl_ext cache will automatically be updated. <p>
 * 
 * This technique requires that JOGL has not been installed in to the
 * JRE under e.g. jre/lib/ext. If problems are seen when deploying
 * this applet launcher, the first question to ask the end user is
 * whether jogl.jar and any associated DLLs, .so's, etc. are installed
 * directly in to the JRE. The applet launcher has been tested
 * primarily under Mozilla and Firefox; there may be problems when
 * running under, for example, Opera. <p>
 *
 * @author Lilian Chamontin
 * @author Kenneth Russell
 */
public class JOGLAppletLauncher extends Applet {
  static {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception ignore) {
    }
  }

  // metadata for native libraries
  private static class NativeLibInfo {
    private String osName;
    private String osArch;
    private String nativeJar;
    private String nativePrefix;
    private String nativeSuffix;

    public NativeLibInfo(String osName, String osArch, String nativeJar, String nativePrefix, String nativeSuffix) {
      this.osName       = osName;
      this.osArch       = osArch;
      this.nativeJar    = nativeJar;
      this.nativePrefix = nativePrefix;
      this.nativeSuffix = nativeSuffix;
    }

    public boolean matchesOSAndArch(String osName, String osArch) {
      if (osName.startsWith(this.osName)) {
        if ((this.osArch == null) ||
            (osArch.startsWith(this.osArch))) {
          return true;
        }
      }
      return false;
    }

    public boolean matchesNativeLib(String nativeLibraryName) {
      if (nativeLibraryName.toLowerCase().endsWith(nativeSuffix)) {
        return true;
      }
      return false;
    }

    public String getNativeJarName() {
      return nativeJar;
    }

    public String getNativeLibName(String baseName) {
      return nativePrefix + baseName + nativeSuffix;
    }

    public boolean isMacOS() {
      return (osName.equals("mac"));
    }
  }

  private static final NativeLibInfo[] allNativeLibInfo = {
    new NativeLibInfo("win",   "x86",   "jogl-natives-windows-i586.jar",     "",    ".dll"),
    new NativeLibInfo("mac",   "ppc",   "jogl-natives-macosx-ppc.jar",       "lib", ".jnilib"),
    new NativeLibInfo("mac",   "i386",  "jogl-natives-macosx-universal.jar", "lib", ".jnilib"),
    new NativeLibInfo("linux", "i386",  "jogl-natives-linux-i586.jar",       "lib", ".so"),
    new NativeLibInfo("linux", "x86",   "jogl-natives-linux-i586.jar",       "lib", ".so"),
    new NativeLibInfo("sunos", "sparc", "jogl-natives-solaris-sparc.jar",    "lib", ".so"),
    new NativeLibInfo("sunos", "x86",   "jogl-natives-solaris-i586.jar",     "lib", ".so")
  };

  private NativeLibInfo nativeLibInfo;
  // Library names computed once the jar comes down.
  // The signatures of these native libraries are checked before
  // installing them.
  private String[] nativeLibNames;
  // Whether the "DRI hack" native library is present and whether we
  // therefore might need to run the DRIHack during loading of the
  // native libraries
  private boolean driHackPresent;

  /** The applet we have to start */
  private Applet subApplet;

  private String subAppletClassName; // from applet PARAM
  private String subAppletDisplayName; // from applet PARAM
  /** URL string to an image used while installing */
  private String subAppletImageName; // from applet PARAM

  private String installDirectory; // (defines a private directory for native libs)

  private JPanel loaderPanel = new JPanel(new BorderLayout());

  private JProgressBar progressBar = new JProgressBar(0,100);

  private boolean isInitOk = false;

  /** false once start() has been invoked */
  private boolean firstStart = true;

  /** true if start() has passed successfully */
  private boolean joglStarted = false;

  public JOGLAppletLauncher() {
  }

  /** Applet initialization */
  public void init()  {

    this.subAppletClassName = getParameter("subapplet.classname");
    if (subAppletClassName == null){
      displayError("Init failed : Missing subapplet.classname argument");
      return;
    }
    this.subAppletDisplayName = getParameter("subapplet.displayname");
    if (subAppletDisplayName == null){
      subAppletDisplayName = "Applet";
    }

    this.subAppletImageName = getParameter("subapplet.image");

    initLoaderLayout();
    validate();

    String codeBase = getCodeBase().toExternalForm().substring(7); // minus http://

    this.installDirectory = codeBase.replace(':', '_')
      .replace('.', '_').replace('/', '_').replace('~','_'); // clean up the name

    String osName = System.getProperty("os.name").toLowerCase();
    String osArch = System.getProperty("os.arch").toLowerCase();
    if (checkOSAndArch(osName, osArch)) {
      this.isInitOk = true;
    } else {
      displayError("Init failed : Unsupported os / arch ( " + osName + " / " + osArch + " )");
    }
  }

  private void displayMessage(String message){
    progressBar.setString(message);
  }

  private void displayError(String errorMessage){
    progressBar.setString("Error : " + errorMessage);
  }

  private void initLoaderLayout(){
    setLayout(new BorderLayout());
    progressBar.setBorderPainted(true);
    progressBar.setStringPainted(true);
    progressBar.setString("Loading...");
    boolean includeImage = false;
    ImageIcon image = null;
    if (subAppletImageName != null){
      try {
        image = new ImageIcon(new URL(subAppletImageName));
        includeImage = true;
      } catch (MalformedURLException ex) {
        ex.printStackTrace();
        // not blocking
      }
    }
    if (includeImage){
      add(loaderPanel, BorderLayout.SOUTH);
      loaderPanel.add(new JLabel(image), BorderLayout.CENTER);
      loaderPanel.add(progressBar, BorderLayout.SOUTH);
    } else {
      add(loaderPanel, BorderLayout.SOUTH);
      loaderPanel.add(progressBar, BorderLayout.CENTER);
    }
  }


  /** start asynchroneous loading of libraries if needed */
  public void start(){
    if (isInitOk){
      if (firstStart) {
        firstStart = false;
        String userHome = System.getProperty("user.home");

        try {
            // We need to load in the jogl package so that we can query the version information
            ClassLoader classloader = getClass().getClassLoader();
            classloader.loadClass("javax.media.opengl.GL");
            Package p = Package.getPackage("javax.media.opengl");

            String installDirName = userHome + File.separator + ".jogl_ext"
              + File.separator + installDirectory + File.separator + p.getImplementationVersion().replace(':', '_');

            final File installDir = new File(installDirName);
 
            Thread refresher = new Thread() {
                public void run() {
                  refreshJOGL(installDir);
                }
            };
            refresher.setPriority(Thread.NORM_PRIORITY - 1);
            refresher.start();
        }  
        catch (ClassNotFoundException e) {
            System.err.println("Unable to load javax.media.opengl package");
            System.exit(0);
        }

      } else if (joglStarted) {
        // we have to start again the applet (start can be called multiple times,
        // e.g once per tabbed browsing
        subApplet.start();
      }
    }
  }

  public void stop(){
    if (subApplet != null){
      subApplet.stop();
    }
  }

  public void destroy(){
    if (subApplet != null){
      subApplet.destroy();
    }
  }


  /** Helper method to make it easier to call methods on the
      sub-applet from JavaScript. */
  public Applet getSubApplet() {
    return subApplet;
  }

  private boolean checkOSAndArch(String osName, String osArch) {
    for (int i = 0; i < allNativeLibInfo.length; i++) {
      NativeLibInfo info = allNativeLibInfo[i];
      if (info.matchesOSAndArch(osName, osArch)) {
        nativeLibInfo = info;
        return true;
      }
    }
    return false;
  }

  /** This method is executed from outside the Event Dispatch Thread, and installs
   *  the required native libraries in the local folder.
   */
  private void refreshJOGL(final File installDir) {
    try {
      Class subAppletClass = Class.forName(subAppletClassName);
      // this will block until the applet jar is downloaded
    } catch (ClassNotFoundException cnfe){
      displayError("Start failed : class not found : " + subAppletClassName);
      return;
    }

    if (!installDir.exists()){
      if (!installDir.mkdirs()) {
        displayError("Unable to create directories for target: " + installDir);
        return;
      }
    }

    String nativeJarName = nativeLibInfo.getNativeJarName();

    URL nativeLibURL;
    URLConnection urlConnection;
    String path = getCodeBase().toExternalForm() + nativeJarName;
    try {
      nativeLibURL = new URL(path);
      urlConnection = nativeLibURL.openConnection();
    } catch (Exception e){
      e.printStackTrace();
      displayError("Couldn't access the native lib URL : " + path);
      return;
    }

    // the timestamp used to determine if we have to download the native jar again
    // don't rely on the OS's timestamp to cache this
    long lastModified = getTimestamp(installDir, urlConnection.getLastModified());
    if (lastModified != urlConnection.getLastModified()) {
      displayMessage("Updating local version of the native libraries");
      // first download the full jar locally
      File localJarFile = new File(installDir, nativeJarName);
      try {
        saveNativesJarLocally(localJarFile, urlConnection);
      } catch (IOException ioe) {
        ioe.printStackTrace();
        displayError("Unable to install the native file locally");
        return;
      }

      try {
        JarFile jf = new JarFile(localJarFile);

        // Iterate the entries finding all candidate libraries that need
        // to have their signatures verified
        if (!findNativeEntries(jf)) {
          displayError("native libraries not found in jar file");
          return;
        }

        byte[] buf = new byte[8192];

        // Go back and verify the signatures
        for (int i = 0; i < nativeLibNames.length; i++) {
          JarEntry entry = jf.getJarEntry(nativeLibNames[i]);
          if (entry == null) {
            displayError("error looking up jar entry " + nativeLibNames[i]);
            return;
          }
          if (!checkNativeCertificates(jf, entry, buf)) {
            displayError("Native library " + nativeLibNames[i] + " isn't properly signed or has other errors");
            return;
          }
        }

        // Now install the native library files
        progressBar.setValue(0);
        for (int i = 0; i < nativeLibNames.length; i++) {
          displayMessage("Installing native files");
          if (!installFile(installDir, jf, nativeLibNames[i], buf)) {
            return;
          }
          int percent = (100 * (i + 1) / nativeLibNames.length);
          progressBar.setValue(percent);
        }

        // At this point we can delete the jar file we just downloaded
        jf.close();
        localJarFile.delete();

        // If installation succeeded, write a timestamp for all of the
        // files to be checked next time
        try {
          File timestampFile = new File(installDir, "timestamp");
          timestampFile.delete();
          BufferedWriter writer = new BufferedWriter(new FileWriter(timestampFile));
          writer.write("" + urlConnection.getLastModified());
          writer.flush();
          writer.close();
        } catch (Exception e) {
          displayError("Error writing time stamp for native libraries");
          return;
        }

      } catch (Exception e) {
        displayError("Error opening jar file " + localJarFile.getName() + " for reading");
        return;
      }
    }

    loadNativesAndStart(installDir);
  }
  
  private long getTimestamp(File installDir, long timestamp) {
    // Avoid returning valid value if timestamp file doesn't exist
    try {
      BufferedReader reader = new BufferedReader(new FileReader(new File(installDir, "timestamp")));
      try {
        StreamTokenizer tokenizer = new StreamTokenizer(reader);
        // Avoid screwing up by not being able to read full longs
        tokenizer.resetSyntax();
        tokenizer.wordChars('0', '9');
        tokenizer.wordChars('-', '-');
        tokenizer.nextToken();
        String tok = tokenizer.sval;
        if (tok != null) {
          return Long.parseLong(tok);
        }
      } catch (Exception e) {
      } finally {
        reader.close();
      }
    } catch (Exception e) {
    }
    return ((timestamp == 0) ? 1 : 0);
  }

  private void saveNativesJarLocally(File localJarFile,
                                     URLConnection urlConnection) throws IOException {
    BufferedOutputStream out = null;;
    InputStream in = null;
    displayMessage("Downloading native library");
    progressBar.setValue(0);
    try {
      out = new BufferedOutputStream(new
                                     FileOutputStream(localJarFile));
      int totalLength = urlConnection.getContentLength();
      in = urlConnection.getInputStream();
      byte[] buffer = new byte[1024];
      int len;
      int sum = 0;
      while ( (len = in.read(buffer)) > 0) {
        out.write(buffer, 0, len);
        sum += len;
        int percent = (100 * sum / totalLength);
        progressBar.setValue(percent);
      }
      out.close();
      in.close();
    } finally {
      // close the files
      if (out != null) {
        try {
          out.close();
        } catch (IOException ignore) {
        }
      }
      if (in != null) {
        try {
          in.close();
        } catch (IOException ignore) {
        }
      }
    }
  }

  private boolean findNativeEntries(JarFile jf) {
    List list = new ArrayList();
    Enumeration e = jf.entries();
    while (e.hasMoreElements()) {
      JarEntry entry = (JarEntry) e.nextElement();
      if (nativeLibInfo.matchesNativeLib(entry.getName())) {
        list.add(entry.getName());
        if (entry.getName().indexOf("jogl_drihack") >= 0) {
          driHackPresent = true;
        }
      }
    }
    if (list.isEmpty()) {
      return false;
    }
    nativeLibNames = (String[]) list.toArray(new String[0]);
    return true;
  }

  /** checking the native certificates with the jogl ones (all must match)*/
  private boolean checkNativeCertificates(JarFile jar, JarEntry entry, byte[] buf){
    // API states that we must read all of the data from the entry's
    // InputStream in order to be able to get its certificates
    try {
      InputStream is = jar.getInputStream(entry);
      int totalLength = (int) entry.getSize();
      int len;
      while ((len = is.read(buf)) > 0) {
      }
      is.close();
      Certificate[] nativeCerts = entry.getCertificates();
      // locate the JOGL certificates
      Certificate[] joglCerts = GLDrawableFactory.class.getProtectionDomain().
        getCodeSource().getCertificates();

      if (nativeCerts == null || nativeCerts.length == 0) {
        return false;
      }
      int checked = 0;
      for (int i = 0; i < joglCerts.length; i++) {
        for (int j = 0; j < nativeCerts.length; j++) {
          if (nativeCerts[j].equals(joglCerts[i])){
            checked++;
            break;
          }
        }
      }
      return  (checked == joglCerts.length);
    } catch (Exception e) {
      return false;
    }
  }

  private boolean installFile(File installDir,
                              JarFile jar,
                              String fileName,
                              byte[] buf) {
    try {
      JarEntry entry = jar.getJarEntry(fileName);
      if (entry == null) {
        displayError("Error finding native library " + fileName);
        return false;
      }
      InputStream is = jar.getInputStream(entry);
      int totalLength = (int) entry.getSize();
      BufferedOutputStream out = null;
      File outputFile = new File(installDir, fileName);
      try {
        out = new BufferedOutputStream(new FileOutputStream(outputFile));
      } catch (Exception e) {
        displayError("Error opening file " + fileName + " for writing");
        return false;
      }      
      int len;
      try {
        while ( (len = is.read(buf)) > 0) {
          out.write(buf, 0, len);
        }
      } catch (IOException ioe) {
        displayError("Error writing file " + fileName + " to disk");
        ioe.printStackTrace();
        outputFile.delete();
        return false;
      }
      out.flush();
      out.close();
      return true;
    } catch (Exception e2) {
      e2.printStackTrace();
      displayError("Error writing file " + fileName + " to disk");
      return false;
    }
  }

  /** last step before launch : System.load() the natives and init()/start() the child applet  */
  private void loadNativesAndStart(final File nativeLibDir) {
    // back to the EDT
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          displayMessage("Loading native libraries");

          // disable JOGL loading from elsewhere
          com.sun.opengl.impl.NativeLibLoader.disableLoading();

          Class driHackClass = null;
          if (driHackPresent) {
            // Load DRI hack library and run the DRI hack itself
            loadLibrary(nativeLibDir, "jogl_drihack");
            try {
              driHackClass = Class.forName("com.sun.opengl.impl.x11.DRIHack");
              driHackClass.getMethod("begin", new Class[] {}).invoke(null, new Object[] {});
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          // Load core JOGL native library
          loadLibrary(nativeLibDir, "jogl");

          if (driHackPresent) {
            // End DRI hack
            try {
              driHackClass.getMethod("end", new Class[] {}).invoke(null, new Object[] {});
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          if (!nativeLibInfo.isMacOS()) { // borrowed from NativeLibLoader
            // Must pre-load JAWT on all non-Mac platforms to
            // ensure references from jogl_awt shared object
            // will succeed since JAWT shared object isn't in
            // default library path
            try {
              System.loadLibrary("jawt");
            } catch (UnsatisfiedLinkError ex) {
              // Accessibility technologies load JAWT themselves; safe to continue
              // as long as JAWT is loaded by any loader
              if (ex.getMessage().indexOf("already loaded") == -1) {
                displayError("Unable to load JAWT");
                throw ex;
              }
            }
          }

          // Load AWT-specific native code
          loadLibrary(nativeLibDir, "jogl_awt");

          displayMessage("Starting applet " + subAppletDisplayName);

          // start the subapplet
          startSubApplet();
        }
      });
  }

  private void loadLibrary(File installDir, String libName) {
    String nativeLibName = nativeLibInfo.getNativeLibName(libName);
    try {
      System.load(new File(installDir, nativeLibName).getPath());
    } catch (UnsatisfiedLinkError ex) {
      // should be safe to continue as long as the native is loaded by any loader
      if (ex.getMessage().indexOf("already loaded") == -1) {
        displayError("Unable to load " + nativeLibName);
        throw ex;
      }
    }
  }

  /** The true start of the sub applet (invoked in the EDT) */
  private void startSubApplet(){
    try {
      subApplet = (Applet)Class.forName(subAppletClassName).newInstance();
      subApplet.setStub(new AppletStubProxy());
    } catch (ClassNotFoundException cnfe) {
      cnfe.printStackTrace();
      displayError("Class not found (" + subAppletClassName + ")");
      return;
    } catch (Exception ex) {
      ex.printStackTrace();
      displayError("Unable to start " + subAppletDisplayName);
      return;
    }

    add(subApplet, BorderLayout.CENTER);

    try {
      subApplet.init();
      remove(loaderPanel);
      validate();
      subApplet.start();
      joglStarted = true;
    } catch (Exception ex){
      ex.printStackTrace();
    }

  }

  /** a proxy to allow the subApplet to work like a real applet */
  class AppletStubProxy implements AppletStub {
    public boolean isActive() {
      return JOGLAppletLauncher.this.isActive();
    }

    public URL getDocumentBase() {
      return JOGLAppletLauncher.this.getDocumentBase();
    }

    public URL getCodeBase() {
      return JOGLAppletLauncher.this.getCodeBase();
    }

    public String getParameter(String name) {
      return JOGLAppletLauncher.this.getParameter(name);
    }

    public AppletContext getAppletContext() {
      return JOGLAppletLauncher.this.getAppletContext();
    }

    public void appletResize(int width, int height) {
      JOGLAppletLauncher.this.resize(width, height);
    }
  }
}

