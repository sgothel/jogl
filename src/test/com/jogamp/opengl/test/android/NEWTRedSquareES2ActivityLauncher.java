package com.jogamp.opengl.test.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class NEWTRedSquareES2ActivityLauncher extends Activity {
       @Override
       public void onCreate(final Bundle savedInstanceState) {
           super.onCreate(savedInstanceState);

           final Uri uri = Uri.parse("launch://jogamp.org/com.jogamp.opengl.test.android.NEWTRedSquareES2Activity?sys=com.jogamp.common&sys=com.jogamp.opengl&pkg=com.jogamp.opengl.test");
           final Intent intent = new Intent("org.jogamp.launcher.action.LAUNCH_ACTIVITY_NORMAL", uri);
           Log.d(getClass().getSimpleName(), "Launching Activity: "+intent);
           startActivity (intent);

           finish(); // done
       }
}
