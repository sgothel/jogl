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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.text.*;
import java.util.*;
import java.util.jar.*;
import javax.swing.*;

import javax.media.opengl.*;


/** This class enables deployment of high-end applets which use OpenGL
 *  for 3D graphics via JOGL and (optionally) OpenAL for spatialized
 *  audio via JOAL. The applet being deployed may be either signed or
 *  unsigned; if it is unsigned, it runs inside the security sandbox,
 *  and if it is signed, the user receives a security dialog to accept
 *  the certificate for the applet as well as for JOGL and JOAL. <P>
 *
 *  The steps for deploying such applets are straightforward. First,
 *  the "archive" parameter to the applet tag must contain jogl.jar
 *  and gluegen-rt.jar, as well as any jar files associated with your
 *  applet (in this case, "your_applet.jar"). <P>
 *
 *  Second, the codebase directory on the server, which contains the
 *  applet's jar files, must also contain jogl.jar, gluegen-rt.jar,
 *  and all of the jogl-natives-*.jar and gluegen-rt-natives-*.jar
 *  files from the standard JOGL and GlueGen runtime distributions
 *  (provided in jogl-[version]-webstart.zip from the <a
 *  href="http://jogl.dev.java.net/servlets/ProjectDocumentList">JOGL
 *  release builds</a> and gluegen-rt-[version]-webstart.zip from the
 *  <a
 *  href="http://gluegen.dev.java.net/servlets/ProjectDocumentList">GlueGen
 *  runtime release builds</a>). Note that the codebase of the applet
 *  is currently the location from which the JOGL native library used
 *  by the applet is downloaded. All of the JOGL and GlueGen-related
 *  jars must be signed by the same entity, which is typically Sun
 *  Microsystems, Inc. <P>
 *
 *  To deploy an applet using both JOGL and JOAL, simply add joal.jar
 *  to the list of jars in the archive tag of the applet, and put
 *  joal.jar and the joal-natives-*.jar signed jars into the same
 *  codebase directory on the web server. These signed jars are
 *  supplied in the joal-[version]-webstart.zip archive from the <a
 *  href="http://joal.dev.java.net/servlets/ProjectDocumentList">JOAL
 *  release builds</a>. <P>
 *
 * Sample applet code:
 * <pre>
 * &lt;applet code="com.sun.opengl.util.JOGLAppletLauncher"
 *      width=600
 *      height=400
 *      codebase="/lib"
 *      archive="jogl.jar,gluegen-rt.jar,your_applet.jar"&gt;
 *   &lt;param name="subapplet.classname" VALUE="untrusted.JOGLApplet"&gt;
 *   &lt;param name="subapplet.displayname" VALUE="My JOGL Applet"&gt;
 *   &lt;param name="progressbar" value="true"&gt;
 *   &lt;param name="cache_archive" VALUE="jogl.jar,gluegen-rt.jar,your_applet.jar"&gt;
 *   &lt;param name="cache_archive_ex" VALUE="jogl.jar;preload,gluegen-rt.jar;preload,your_applet.jar;preload"&gt;
 * &lt;/applet&gt;
 * </pre>
 * <p>
 * 
 * There are some limitations with this approach. It is not possible
 * to specify e.g. -Dsun.java2d.noddraw=true or
 * -Dsun.java2d.opengl=true for better control over the Java2D
 * pipeline as it is with Java Web Start. However, the
 * JOGLAppletLauncher tries to force the use of
 * -Dsun.java2d.noddraw=true on Windows platforms for best robustness
 * by detecting if it has not been set and asking the user whether it
 * can update the Java Plug-In configuration automatically. If the
 * user agrees to this, a browser restart is required in order for the
 * change to take effect, though it is permanent for subsequent
 * browser restarts. <P>
 * 
 * The behavior of the noddraw-related dialog box can be changed via
 * two applet parameters. The <CODE>jogl.silent.noddraw.check</CODE>
 * parameter, if set to <CODE>"true"</CODE>, silences the two dialog
 * boxes associated with this check, forcing it to always be performed
 * and deployment.properties to be silently updated if necessary
 * (unless the user previously saw such a dialog box and dismissed it
 * by saying "No, Don't Ask Again"). The noddraw check can be disabled
 * completely by setting the <CODE>jogl.disable.noddraw.check</CODE>
 * applet parameter to <CODE>"true"</CODE>. <P>
 * 
 * The JOGL (and optionally JOAL) natives are cached in the user's
 * home directory (the value of the "user.home" system property in
 * Java) under the directory .jogl_ext. The Java Plug-In is
 * responsible for performing all other jar caching. If the JOGL
 * installation is updated on the server, the .jogl_ext cache will
 * automatically be updated. <p>
 * 
 * This technique requires that JOGL has not been installed in to the
 * JRE under e.g. jre/lib/ext. If problems are seen when deploying
 * this applet launcher, the first question to ask the end user is
 * whether jogl.jar and any associated DLLs, .so's, etc. are installed
 * directly in to the JRE. The applet launcher has been tested
 * primarily under Mozilla, Firefox and Internet Explorer; there may
 * be problems when running under, for example, Opera. <p>
 *
 * It has been discovered that the Talkback agent in Mozilla / Firefox
 * has bad interactions with OpenGL applets. For highest performance,
 * we recommend disabling the Talkback agent; find talkback.exe, run
 * it, and follow the directions for turning it off. Please see
 * <a href="http://www.javagaming.org/forums/index.php?topic=12200.30">this
 * thread</a> on the javagaming.org forums and
 * <a href="https://bugzilla.mozilla.org/show_bug.cgi?id=326381">this
 * thread</a> on the Mozilla bug reporting database. <p>
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
    private String osNameAndArchPair;
    private String nativePrefix;
    private String nativeSuffix;

    public NativeLibInfo(String osName, String osArch, String osNameAndArchPair, String nativePrefix, String nativeSuffix) {
      this.osName       = osName;
      this.osArch       = osArch;
      this.osNameAndArchPair = osNameAndArchPair;
      this.nativePrefix = nativePrefix;
      this.nativeSuffix = nativeSuffix;
    }

    public boolean matchesOSAndArch(String osName, String osArch) {
      if (osName.toLowerCase().startsWith(this.osName)) {
        if ((this.osArch == null) ||
            (osArch.toLowerCase().equals(this.osArch))) {
          return true;
        }
      }
      return false;
    }

    public boolean matchesNativeLib(String fileName) {
      if (fileName.toLowerCase().endsWith(nativeSuffix)) {
        return true;
      }
      return false;
    }

    public String formatNativeJarName(String nativeJarPattern) {
      return MessageFormat.format(nativeJarPattern, new Object[] { osNameAndArchPair });
    }

    public String getNativeLibName(String baseName) {
      return nativePrefix + baseName + nativeSuffix;
    }

    public boolean isMacOS() {
      return (osName.equals("mac"));
    }

    public boolean mayNeedDRIHack() {
      return (!isMacOS() && !osName.equals("win"));
    }
  }

  private static final NativeLibInfo[] allNativeLibInfo = {
    new NativeLibInfo("win",   "x86",   "windows-i586",     "",    ".dll"),
    new NativeLibInfo("win",   "amd64", "windows-amd64",    "",    ".dll"),
    new NativeLibInfo("win",   "x86_64","windows-amd64",    "",    ".dll"),
    new NativeLibInfo("mac",   "ppc",   "macosx-ppc",       "lib", ".jnilib"),
    new NativeLibInfo("mac",   "i386",  "macosx-universal", "lib", ".jnilib"),
    new NativeLibInfo("linux", "i386",  "linux-i586",       "lib", ".so"),
    new NativeLibInfo("linux", "x86",   "linux-i586",       "lib", ".so"),
    new NativeLibInfo("linux", "amd64", "linux-amd64",      "lib", ".so"),
    new NativeLibInfo("linux", "x86_64","linux-amd64",      "lib", ".so"),
    new NativeLibInfo("sunos", "sparc", "solaris-sparc",    "lib", ".so"),
    new NativeLibInfo("sunos", "sparcv9","solaris-sparcv9", "lib", ".so"),
    new NativeLibInfo("sunos", "x86",   "solaris-i586",     "lib", ".so"),
    new NativeLibInfo("sunos", "amd64", "solaris-amd64",    "lib", ".so"),
    new NativeLibInfo("sunos", "x86_64","solaris-amd64",    "lib", ".so")
  };

  private NativeLibInfo nativeLibInfo;
  // Library names computed once the jar comes down.
  // The signatures of these native libraries are checked before
  // installing them.
  private String[] nativeLibNames;

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

  /** Indicates whether JOAL is present */
  private boolean haveJOAL = false;

  // Helpers for question about whether to update deployment.properties
  private static final String JRE_PREFIX = "deployment.javapi.jre.";
  private static final String NODDRAW_PROP = "-Dsun.java2d.noddraw=true";
  private static final String DONT_ASK = ".dont_ask";

  public JOGLAppletLauncher() {
  }

  private static String md2Hash(String str) {
    // Helps hash the jars in the "archive" tag into a hex value to
    // avoid having too-long path names in the install directory's
    // path name but also to have unique directories for each
    // different archive set used (also meaning for each class loader
    // loading something via the JOGLAppletLauncher) -- note that this
    // is somewhat dependent on the Sun implementation of applets and
    // their class loaders
    MessageDigest md2 = null;
    try {
      md2 = MessageDigest.getInstance("MD2");
    } catch (NoSuchAlgorithmException e) {
      return "";
    }
    byte[] digest = md2.digest(str.getBytes());
    if (digest == null || (digest.length == 0))
      return "";
    StringBuffer res = new StringBuffer();
    for (int i = 0; i < digest.length; i++) {
      res.append(Integer.toHexString(digest[i] & 0xFF));
    }
    return res.toString();
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

    String extForm = getCodeBase().toExternalForm();
    String codeBase = extForm.substring(extForm.indexOf(":") + 3);  // minus http:// or https://

    this.installDirectory = codeBase.replace(':', '_')
      .replace('.', '_').replace('/', '_').replace('~','_') // clean up the name
      + md2Hash(getParameter("archive")); // make it unique across different applet class loaders

    String osName = System.getProperty("os.name");
    String osArch = System.getProperty("os.arch");
    if (checkOSAndArch(osName, osArch)) {
      this.isInitOk = true;
    } else {
      displayError("Init failed : Unsupported os / arch ( " + osName + " / " + osArch + " )");
    }
  }

  private void displayMessage(final String message){
    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  progressBar.setString(message);
	}
      });
  }

  private void displayError(final String errorMessage){
    // Print message to Java console too in case it's truncated in the applet's display
    System.err.println(errorMessage);
    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  progressBar.setString("Error : " + errorMessage);
	}
      });
  }

  private void setProgress(final int value) {
    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  progressBar.setValue(value);
	}
      });
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
        checkNoDDrawAndUpdateDeploymentProperties();
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

  // Get a "boolean" parameter, assuming that anything non-null aside
  // from "false" is true
  private boolean getBooleanParameter(String parameterName) {
    String val = getParameter(parameterName);
    if (val == null)
      return false;
    return !val.toLowerCase().equals("false");
  }

  private void checkNoDDrawAndUpdateDeploymentProperties() {
    if (getBooleanParameter("jogl.disable.noddraw.check"))
      return;
    if (System.getProperty("os.name").toLowerCase().startsWith("windows") &&
        !"true".equalsIgnoreCase(System.getProperty("sun.java2d.noddraw"))) {
      if (!SwingUtilities.isEventDispatchThread()) {
        try {
          SwingUtilities.invokeAndWait(new Runnable() {
              public void run() {
                updateDeploymentPropertiesImpl();
              }
            });
        } catch (Exception e) {
        }
      } else {
        updateDeploymentPropertiesImpl();
      }
    }
  }

  private void updateDeploymentPropertiesImpl() {
    String userHome = System.getProperty("user.home");
    File dontAskFile = new File(userHome + File.separator + ".jogl_ext" +
                                File.separator + DONT_ASK);
    if (dontAskFile.exists())
      return; // User asked us not to prompt again

    int option = 0;

    if (!getBooleanParameter("jogl.silent.noddraw.check")) {
      option = JOptionPane.showOptionDialog(null,
                                            "For best robustness of JOGL applets on Windows,\n" +
                                            "we recommend disabling Java2D's use of DirectDraw.\n" +
                                            "This setting will affect all applets, but is unlikely\n" +
                                            "to slow other applets down significantly. May we update\n" +
                                            "your deployment.properties to turn off DirectDraw for\n" +
                                            "applets? You can change this back later if necessary\n" +
                                            "using the Java Control Panel, Java tab, under Java\n" +
                                            "Applet Runtime Settings.",
                                            "Update deployment.properties?",
                                            JOptionPane.YES_NO_CANCEL_OPTION,
                                            JOptionPane.QUESTION_MESSAGE,
                                            null,
                                            new Object[] {
                                              "Yes",
                                              "No",
                                              "No, Don't Ask Again"
                                            },
                                            "Yes");
    }

    if (option < 0 ||
        option == 1)
      return; // No

    if (option == 2) {
      try {
        dontAskFile.createNewFile();
      } catch (IOException e) {
      }
      return; // No, Don't Ask Again
    }

    try {
      // Must update deployment.properties
      File propsDir = new File(System.getProperty("user.home") + File.separator +
                               "Application Data/Sun/Java/Deployment");
      if (!propsDir.exists())
        // Don't know what's going on or how to set this permanently
        return;

      File propsFile = new File(propsDir, "deployment.properties");
      if (!propsFile.exists())
        // Don't know what's going on or how to set this permanently
        return;

      Properties props = new Properties();
      InputStream input = new BufferedInputStream(new FileInputStream(propsFile));
      props.load(input);
      input.close();
      // Search through the keys looking for JRE versions
      Set/*<String>*/ jreVersions = new HashSet/*<String>*/();
      for (Iterator/*<String>*/ iter = props.keySet().iterator(); iter.hasNext(); ) {
        String key = (String) iter.next();
        if (key.startsWith(JRE_PREFIX)) {
          int idx = key.lastIndexOf(".");
          if (idx >= 0 && idx > JRE_PREFIX.length()) {
            String jreVersion = key.substring(JRE_PREFIX.length(), idx);
            jreVersions.add(jreVersion);
          }
        }
      }

      // Make sure the currently-running JRE shows up in this set to
      // avoid repeated displays of the dialog. It might not in some
      // upgrade scenarios where there was a pre-existing
      // deployment.properties and the new Java Control Panel hasn't
      // been run yet.
      jreVersions.add(System.getProperty("java.version"));

      // OK, now that we know all JRE versions covered by the
      // deployment.properties, check out the args for each and update
      // them
      for (Iterator/*<String>*/ iter = jreVersions.iterator(); iter.hasNext(); ) {
        String version = (String) iter.next();
        String argKey = JRE_PREFIX + version + ".args";
        String argVal = props.getProperty(argKey);
        if (argVal == null) {
          argVal = NODDRAW_PROP;
        } else if (argVal.indexOf(NODDRAW_PROP) < 0) {
          argVal = argVal + " " + NODDRAW_PROP;
        }
        props.setProperty(argKey, argVal);
      }

      OutputStream output = new BufferedOutputStream(new FileOutputStream(propsFile));
      props.store(output, null);
      output.close();

      if (!getBooleanParameter("jogl.silent.noddraw.check")) {
        // Tell user we're done
        JOptionPane.showMessageDialog(null,
                                      "For best robustness, we recommend you now exit and\n" +
                                      "restart your web browser. (Note: clicking \"OK\" will\n" +
                                      "not exit your browser.)",
                                      "Browser Restart Recommended",
                                      JOptionPane.INFORMATION_MESSAGE);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
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

    // See whether JOAL is present
    try {
      Class alClass = Class.forName("net.java.games.joal.AL", false, this.getClass().getClassLoader());
      haveJOAL = true;
      // Note: it seems that some JRE implementations can throw
      // SecurityException as well as ClassNotFoundException, at least
      // if the OpenAL classes are not present and the web server
      // redirects elsewhere
    } catch (Exception e) {
    }

    String[] nativeJarNames = new String[] {
      nativeLibInfo.formatNativeJarName("jogl-natives-{0}.jar"),
      nativeLibInfo.formatNativeJarName("gluegen-rt-natives-{0}.jar"),
      (haveJOAL ? nativeLibInfo.formatNativeJarName("joal-natives-{0}.jar") : null)
    };

    for (int n = 0; n < nativeJarNames.length; n++) {
      String nativeJarName = nativeJarNames[n];

      if (nativeJarName == null)
        continue;

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
      long lastModified = getTimestamp(installDir, nativeJarName, urlConnection.getLastModified());
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
	  setProgress(0);
	  for (int i = 0; i < nativeLibNames.length; i++) {
	    displayMessage("Installing native files from " + nativeJarName);
	    if (!installFile(installDir, jf, nativeLibNames[i], buf)) {
	      return;
	    }
	    int percent = (100 * (i + 1) / nativeLibNames.length);
	    setProgress(percent);
	  }

	  // At this point we can delete the jar file we just downloaded
	  jf.close();
	  localJarFile.delete();

	  // If installation succeeded, write a timestamp for all of the
	  // files to be checked next time
	  try {
            File timestampFile = new File(installDir, getTimestampFileName(nativeJarName));
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
    }

    loadNativesAndStart(installDir);
  }
  
  private String getTimestampFileName(String nativeJarName) {
    return "timestamp-" + nativeJarName.replace('.', '-');
  }

  private long getTimestamp(File installDir, String nativeJarName, long timestamp) {
    // Avoid returning valid value if timestamp file doesn't exist
    try {
      String timestampName = getTimestampFileName(nativeJarName);
      BufferedReader reader = new BufferedReader(new FileReader(new File(installDir, timestampName)));
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
    setProgress(0);
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
        setProgress(percent);
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
      boolean exists = false;
      try {
        exists = outputFile.exists();
        out = new BufferedOutputStream(new FileOutputStream(outputFile));
      } catch (Exception e) {
        if (exists) {
          // It's possible the files were updated on the web server
          // but we still have them loaded in this process; skip this
          // update
          return true;
        } else {
          displayError("Error opening file " + fileName + " for writing");
          return false;
        }
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
      is.close();
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

          // disable JOGL and GlueGen runtime library loading from elsewhere
          com.sun.opengl.impl.NativeLibLoader.disableLoading();
          com.sun.gluegen.runtime.NativeLibLoader.disableLoading();

	  // Open GlueGen runtime library optimistically. Note that
	  // currently we do not need this on any platform except X11
	  // ones, because JOGL doesn't use the GlueGen NativeLibrary
	  // class anywhere except the DRIHack class, but if for
	  // example we add JOAL support then we will need this on
	  // every platform.
	  loadLibrary(nativeLibDir, "gluegen-rt");

          Class driHackClass = null;
          if (nativeLibInfo.mayNeedDRIHack()) {
            // Run the DRI hack
            try {
              driHackClass = Class.forName("com.sun.opengl.impl.x11.DRIHack");
              driHackClass.getMethod("begin", new Class[] {}).invoke(null, new Object[] {});
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          // Load core JOGL native library
          loadLibrary(nativeLibDir, "jogl");

          if (nativeLibInfo.mayNeedDRIHack()) {
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

          if (haveJOAL) {
            // Turn off the System.loadLibrary call of the joal_native
            // library. It will still need to load the OpenAL library
            // internally via another mechanism.
            try {
              Class c = Class.forName("net.java.games.joal.impl.NativeLibLoader");
              c.getMethod("disableLoading", new Class[] {}).invoke(null, new Object[] {});
            } catch (Exception e) {
              e.printStackTrace();
            }

            // Append the installed native library directory to
            // java.library.path. This is the most convenient way to
            // make this directory available to the NativeLibrary code,
            // which needs it for loading OpenAL if present.
            String javaLibPath = System.getProperty("java.library.path");
            String absPath = nativeLibDir.getAbsolutePath();
            boolean shouldSet = false;
            if (javaLibPath == null) {
              javaLibPath = absPath;
              shouldSet = true;
            } else if (javaLibPath.indexOf(absPath) < 0) {
              javaLibPath = javaLibPath + File.pathSeparator + absPath;
              shouldSet = true;
            }
            if (shouldSet) {
              System.setProperty("java.library.path", javaLibPath);
            }

            // Load core JOAL native library
            loadLibrary(nativeLibDir, "joal_native");
          }

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
      // Note: if we have loaded this particular copy of the
      // JOGL-related native library in another class loader, the
      // steps taken above to ensure the installation directory name
      // was unique have failed. We can't continue properly in this
      // case, so just print and re-throw the exception.
      ex.printStackTrace();
      throw ex;
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
      checkNoDDrawAndUpdateDeploymentProperties();
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

