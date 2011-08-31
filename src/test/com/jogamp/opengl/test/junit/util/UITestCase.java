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

import com.jogamp.common.util.locks.SingletonInstance;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.rules.TestName;


public abstract class UITestCase {
    @Rule public TestName _unitTestName = new TestName();

    public static final String SINGLE_INSTANCE_LOCK_FILE = "UITestCase.lock";
    public static final int SINGLE_INSTANCE_LOCK_PORT = 59999;
    
    public static final long SINGLE_INSTANCE_LOCK_TO   = 3*60*1000; // wait up to 3 min
    public static final long SINGLE_INSTANCE_LOCK_POLL =      1000; // poll every 1s

    static volatile SingletonInstance singletonInstance;

    private static final synchronized void initSingletonInstance() {
        if( null == singletonInstance )  {
            // singletonInstance = SingletonInstance.createFileLock(SINGLE_INSTANCE_LOCK_POLL, SINGLE_INSTANCE_LOCK_FILE);
            singletonInstance = SingletonInstance.createServerSocket(SINGLE_INSTANCE_LOCK_POLL, SINGLE_INSTANCE_LOCK_PORT);
            if(!singletonInstance.tryLock(SINGLE_INSTANCE_LOCK_TO)) {
                throw new RuntimeException("Fatal: Could lock single instance: "+singletonInstance.getName());
            }
        }
    }

    public final String getTestMethodName() {
        return _unitTestName.getMethodName();
    }
    
    public final String getSimpleTestName() {
        return getClass().getSimpleName()+" - "+getTestMethodName();
    }

    public final String getFullTestName() {
        return getClass().getName()+" - "+getTestMethodName();
    }
    
    @BeforeClass
    public static void oneTimeSetUp() {
        // one-time initialization code                
        initSingletonInstance();
    }

    @AfterClass
    public static void oneTimeTearDown() {
        // one-time cleanup code
        System.gc(); // force cleanup
        singletonInstance.unlock();
    }

    @Before
    public void setUp() {
        System.err.println("++++ UITestCase.setUp: "+getFullTestName());
    }

    @After
    public void tearDown() {
        System.err.println("++++ UITestCase.tearDown: "+getFullTestName());
    }

}

