package com.github.mrthreepwood.tiltball.tiltball;

import android.app.Activity;
import android.content.Context;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;

import java.util.Timer;
import java.util.TimerTask;

public class TiltBallActivity extends Activity implements SensorEventListener, View.OnTouchListener {

    BallView mBallView = null;
    float accelCoeff = .06f;
    PointF mBallPos = new PointF(0,0);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE); //hide title bar
        //set app to full screen and keep screen on
        getWindow().setFlags(0xFFFFFFFF,
                LayoutParams.FLAG_FULLSCREEN | LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //create pointer to main screen
        final FrameLayout mainView =
                (android.widget.FrameLayout) findViewById(R.id.main_view);
        //create initial ball
        mBallView = new BallView(this);

        mainView.addView(mBallView); //add ball to main screen
        mBallView.invalidate(); //call onDraw in BallView
        mBallView.view = mBallView;
        //listener for accelerometer, use anonymous class for simplicity
        ((SensorManager) getSystemService(Context.SENSOR_SERVICE)).registerListener(this,
                ((SensorManager) getSystemService(Context.SENSOR_SERVICE)).getSensorList(Sensor.TYPE_ACCELEROMETER).get(0),
                SensorManager.SENSOR_DELAY_GAME);
        //listener for touch event
        //Draws ball based on touch? Why is this in OnCreate?
        mainView.setOnTouchListener(this);
    }

    //For state flow see http://developer.android.com/reference/android/app/Activity.html
    @Override
    public void onPause() //app moved to background, stop background threads
    {
        mBallView.mTmr.cancel(); //kill\release timer (our only background thread)
        mBallView.mTmr = null;
        super.onPause();
    }

    @Override
    public void onResume() //app moved to foreground (also occurs at app startup)
    {
        super.onResume();
        //create timer to move ball to new position
         //start timer
        mBallView.startTimer();
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        //set ball speed based on phone tilt (ignore Z axis)
        mBallView.mBallAcl.x = -event.values[0]*accelCoeff;
        mBallView.mBallAcl.y = event.values[1]*accelCoeff;
        //timer event will redraw ball
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //set ball position based on screen touch
        int x = (int) event.getX();
        int y = (int) event.getY();

        if (x > 10 && x < mBallView.mScrWidth - 10 && y > 10 && y < mBallView.mScrHeight -10)
            mBallView.reset();
        //timer event will redraw ball
        return true;
    }
}