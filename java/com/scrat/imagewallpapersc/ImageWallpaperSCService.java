package com.scrat.imagewallpapersc;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.service.wallpaper.WallpaperService;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class ImageWallpaperSCService extends OpenGLES2WallpaperService{
    @Override
    GLSurfaceView.Renderer getNewRenderer(Context context) {
        return new ImageWallpaperSCRender(context);
    }
}

abstract class OpenGLES2WallpaperService extends ImageWallpaperSC {

    @Override
    public Engine onCreateEngine() {
        return new OpenGLES2Engine();
    }
    class OpenGLES2Engine extends ImageWallpaperSCService.GLEngine {

        @Override
        public void onSurfaceCreated(SurfaceHolder surfaceHolder) {
            super.onSurfaceCreated(surfaceHolder);
            final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            assert activityManager != null;
            final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
            final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
            if (supportsEs2)
            {
                setEGLContextClientVersion();
                setPreserveEGLContextOnPause();
                setRenderer(getNewRenderer(getContext()));
            }
        }

    }
    abstract GLSurfaceView.Renderer getNewRenderer(Context context);
}

abstract class ImageWallpaperSC extends WallpaperService {

    private GestureDetector gestureDetector;
    private SensorManager mSensorManager;
    private Sensor mGyroscope;

    public class GLEngine extends Engine implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_GYROSCOPE)
                return;
            mRender.Sensor_Event(event.values[1]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        void createSensor(Context context) {
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (mSensorManager != null) {
                mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_UI);
            }
        }
        void destroySensor(){
            try {
                if (mSensorManager != null) mSensorManager.unregisterListener(this);
            } catch (Exception ignore){

            }
            mSensorManager = null;
            mGyroscope = null;
        }

        class WallpaperGLSurfaceView extends GLSurfaceView {
            WallpaperGLSurfaceView(Context context) {
                super(context);
                gestureDetector = new GestureDetector(context, new GestureListener());
            }

            @Override
            public SurfaceHolder getHolder() {
                return getSurfaceHolder();
            }

            void onDestroy() {
                super.onDetachedFromWindow();
            }
        }

        Context getContext(){
            return ImageWallpaperSC.this;
        }

        private WallpaperGLSurfaceView glSurfaceView;
        private boolean rendererHasBeenSet;
        private ImageWallpaperSCRender mRender;

        @Override
        public void onSurfaceCreated(SurfaceHolder surfaceHolder) {
            super.onSurfaceCreated(surfaceHolder);
            glSurfaceView = new WallpaperGLSurfaceView(getContext());
        }



        @Override
        public void onVisibilityChanged(boolean visible) {
            if (rendererHasBeenSet) {
                if (visible) {
                    mRender.loadParam(getContext());
                    if (mRender.getTiltSetting()) createSensor(getContext());
                    glSurfaceView.onResume();
                    gestureDetector = new GestureDetector(getContext(), new GestureListener());
                    mRender.onResume();
                } else {
                    destroySensor();
                    glSurfaceView.onPause();
                    gestureDetector = null;
                    mRender.onPause();
                }
            }
            super.onVisibilityChanged(visible);
        }

        @Override
        public void onDestroy() {
            try {
                super.onDestroy(); //NullPointerException
                glSurfaceView.onDestroy();
                destroySensor();
                mRender.onDestroy();
            } catch (Exception ignore) {}
        }

        void setRenderer(GLSurfaceView.Renderer renderer) {
                mRender = (ImageWallpaperSCRender) renderer;
                glSurfaceView.setRenderer(renderer);
                rendererHasBeenSet = true;
        }

        void setPreserveEGLContextOnPause() {
            glSurfaceView.setPreserveEGLContextOnPause(true);
        }

        void setEGLContextClientVersion() {
            glSurfaceView.setEGLContextClientVersion(2);
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
            if (gestureDetector != null) gestureDetector.onTouchEvent(event);
            mRender.onTouchEvent(event);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                                     float xOffsetStep, float yOffsetStep, int xPixelOffset,
                                     int yPixelOffset) {
            mRender.onOffsetsChanged(xOffset);
        }

        private class GestureListener extends GestureDetector.SimpleOnGestureListener {

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                mRender.loadParam(getContext());

                if (mRender.getTiltSetting()) {
                    if (mSensorManager == null) createSensor(getContext());
                } else if (mSensorManager != null) destroySensor();

                if (mRender.getDoubleTapSetting()) mRender.change();
                return true;
            }
        }

    }



}
