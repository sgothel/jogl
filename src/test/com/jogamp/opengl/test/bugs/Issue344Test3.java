package com.jogamp.opengl.test.bugs;

public class Issue344Test3 extends Issue344Base {
    protected String getText() {
        // test 3 - slight rendering artifacts around very large letters
        return "abcde";
    }

    public static void main(final String[] args) {
        new Issue344Test3().run(args);
    }
}
