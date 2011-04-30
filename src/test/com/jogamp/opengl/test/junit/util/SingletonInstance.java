/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
 
package com.jogamp.opengl.test.junit.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Date;

public class SingletonInstance {

    static final boolean DEBUG = true;
    static final String temp_file_path;

    static {
        String s = null;
        try {
            File tmpFile = File.createTempFile("TEST", "tst");
            String absTmpFile = tmpFile.getCanonicalPath();
            tmpFile.delete();
            s = absTmpFile.substring(0, absTmpFile.lastIndexOf(File.separator));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        temp_file_path = s;
    }

    public static String getCanonicalTempPath() {
        return temp_file_path;
    }

    public static String getCanonicalTempLockFilePath(String basename) {
        return getCanonicalTempPath() + File.separator + basename;
    }

    public SingletonInstance(String name, String lockFileBasename) {
        this.name = name;
        this.file = new File ( getCanonicalTempLockFilePath ( lockFileBasename ) );
        setupFileCleanup();
    }

    public SingletonInstance(String name, File lockFile) {
        this.name = name;
        this.file = lockFile ;
        setupFileCleanup();
    }

    public String getName() { return name; }
    
    void setupFileCleanup() {
        file.deleteOnExit();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                unlock();
            }
        });        
    }

    public synchronized void lock(long timeout_ms, long poll_ms) {
        long i=0;
        try {
            do {
                if(tryLock()) {
                    return;
                }
                if(DEBUG && 0==i) {
                    System.err.println("SLOCK "+System.currentTimeMillis()+" ??? "+name+" - Wait for lock " + file);
                }
                i++;
                Thread.sleep(poll_ms);
            } while ( i < timeout_ms / poll_ms ) ;
        } catch ( InterruptedException ie ) {
            throw new  RuntimeException(ie);
        }
        throw new RuntimeException("SLOCK "+System.currentTimeMillis()+" EEE "+name+" - couldn't get lock within "+timeout_ms+"ms");
    }

    public synchronized boolean tryLock() {
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            fileLock = randomAccessFile.getChannel().tryLock();

            if (fileLock != null) {
                locked = true;
                if(DEBUG) {
                    System.err.println("SLOCK "+System.currentTimeMillis()+" +++ "+name+" - Locked " + file);
                }
                return true;
            }
        } catch (Exception e) {
            System.err.println("SLOCK "+System.currentTimeMillis()+" EEE "+name+" - Unable to create and/or lock file: " + file);
            e.printStackTrace();
        }
        return false;
    }

    public synchronized boolean unlock() {
        try {
            if(null != fileLock) {
                if(locked) {
                    fileLock.release();
                    if(DEBUG) {
                        System.err.println("SLOCK "+System.currentTimeMillis()+" --- "+name+" - Unlocked " + file);
                    }        
                }
                fileLock = null;
            }
            if(null != randomAccessFile) {
                randomAccessFile.close();
                randomAccessFile = null;
            }
            if(null != file) {
                file.delete();
                file = null;
            }
            return true;
        } catch (Exception e) {
            System.err.println("SLOCK "+System.currentTimeMillis()+" EEE "+name+" - Unable to remove lock file: " + file);
            e.printStackTrace();
        } finally {
            fileLock = null;
            randomAccessFile = null;
            locked = false;
        }
        return false;
    }

    public synchronized boolean isLocked() {
        return locked;
    }

    String name;
    File file = null;
    RandomAccessFile randomAccessFile = null;
    FileLock fileLock = null;
    boolean locked = false;
}
