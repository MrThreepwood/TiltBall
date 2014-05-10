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
    Handler mHandler = new Handler(); //so redraw occurs in main thread
    Timer mTmr = null;
    int mScrWidth, mScrHeight;
    float dragCoeff = .02f;
    float accelCoeff = .06f;
    PointF mBallPos = new PointF(0,0);
    PointF mBallSpd = new PointF(0,0);
    PointF mBallAcl = new PointF(0,0);

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
        //get screen dimensions
        Display display = getWindowManager().getDefaultDisplay();
        mScrWidth = display.getWidth();
        mScrHeight = display.getHeight();
        mBallPos = new android.graphics.PointF();
        mBallAcl = new android.graphics.PointF();

        //create variables for ball position and speed
        mBallPos.x = mScrWidth / 2;
        mBallPos.y = mScrHeight / 2;
        mBallAcl.x = 0;
        mBallAcl.y = 0;
        //create initial ball
        mBallView = new BallView(this, mBallPos.x, mBallPos.y, 10);

        mainView.addView(mBallView); //add ball to main screen
        mBallView.invalidate(); //call onDraw in BallView
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
        mTmr.cancel(); //kill\release timer (our only background thread)
        mTmr = null;
        super.onPause();
    }

    @Override
    public void onResume() //app moved to foreground (also occurs at app startup)
    {
        super.onResume();
        //create timer to move ball to new position
        mTmr = new Timer();
        mTmr.schedule(new MyTimerTask(), 10, 10); //start timer
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        //set ball speed based on phone tilt (ignore Z axis)
        mBallAcl.x = -event.values[0]*accelCoeff;
        mBallAcl.y = event.values[1]*accelCoeff;
        //timer event will redraw ball
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //set ball position based on screen touch
        mBallPos.x = event.getX();
        mBallPos.y = event.getY();
        //timer event will redraw ball
        return true;
    }

    class MyTimerTask extends TimerTask {
        public void run() {
            //if debugging with external device,
            //  a log cat viewer will be needed on the device
            //android.util.Log.d("TiltBall", "Timer Hit - " + mBallPos.x + ":" + mBallPos.y);
            //move ball based on current speed
            if ((mBallAcl.x < .02f && mBallAcl.x > 0) || (mBallAcl.x > -.02f && mBallAcl.x < 0))
                mBallAcl.x =0;
            if ((mBallAcl.y < .02f && mBallAcl.y > 0) || (mBallAcl.y > -.02f && mBallAcl.y < 0))
                mBallAcl.y =0;
            mBallAcl.x -= dragCoeff*mBallSpd.x;
            mBallAcl.y -= dragCoeff*mBallSpd.y;
            mBallSpd.x += mBallAcl.x;
            mBallSpd.y += mBallAcl.y;
            mBallPos.x += mBallSpd.x;
            mBallPos.y += mBallSpd.y;
            //if ball goes off screen, reposition to opposite side of screen
            if (mBallPos.x > mScrWidth){
                mBallPos.x = mScrWidth;
                mBallSpd.x = -mBallSpd.x;
            }
            if (mBallPos.y > mScrHeight){
                mBallSpd.y = -mBallSpd.y;
                mBallPos.y = mScrHeight;
            }
            if (mBallPos.x < 0) {
                mBallSpd.x = -mBallSpd.x;
                mBallPos.x = 0;
            }
            if (mBallPos.y < 0){
                mBallPos.y = 0;
                mBallSpd.y = -mBallSpd.y;
            }

            //update ball class instance
            mBallView.x = mBallPos.x;
            mBallView.y = mBallPos.y;
            //redraw ball. Must run in background thread to prevent thread lock.
            mHandler.post(new Runnable() {
                public void run() {
                    mBallView.invalidate();
                }
            });
        }
    }
}