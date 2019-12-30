package com.scrat.imagewallpapersc;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
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

    public class GLEngine extends Engine {
        class WallpaperGLSurfaceView extends GLSurfaceView {
            WallpaperGLSurfaceView(Context context) {
                super(context);
                gestureDetector = new GestureDetector(context, new GestureListener());

            }
            @Override
            public SurfaceHolder getHolder() {
                return getSurfaceHolder();
            }

            public void onDestroy() {
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
                    glSurfaceView.onResume();
                    gestureDetector = new GestureDetector(getContext(), new GestureListener());
                    mRender.onResume();
                } else {
                    glSurfaceView.onPause();
                    gestureDetector = null;
                    mRender.onPause();
                }
            }
            super.onVisibilityChanged(visible);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            glSurfaceView.onDestroy();
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
                long now = SystemClock.uptimeMillis();
                mRender.TimeChange = now - (mRender.Timer * 60000);
                return true;
            }
        }
    }

}
