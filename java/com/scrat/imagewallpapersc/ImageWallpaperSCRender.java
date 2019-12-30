package com.scrat.imagewallpapersc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.VelocityTracker;
import android.view.WindowManager;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

class ImageWallpaperSCMediaTexture {
    protected enum FileType {
        PICTURE, VIDEO, NONE
    }

    private int[] max;
    private String filePath;
    private FileType fileType = FileType.NONE;
    private MediaPlayer mediaPlayer;
    private int width;
    private int height;
    private SurfaceTexture mSurface;
    private int sp_Image;
    private float alpha;
    private boolean error;
    private Bitmap mBitmap;
    private int[] mTexture = new int[1];
    private float picturePosition;

    private float max_speed;
    private float motion_speed;
    private boolean vector;
    private float speed;
    private final int screenCount = 5;
    private Point mScreenSize;
    private int indexLayer;
    private int touchType;
    private int quality;


    private FloatBuffer uvBuffer;
    private final float[] mtrxProjection = new float[16];
    private final float[] mtrxView = new float[16];
    private final float[] mtrxProjectionAndView = new float[16];
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    private final short[] indices = new short[]{0, 1, 2, 0, 2, 3};
    private int fragmentShader;
    private int vertexShader;


    ImageWallpaperSCMediaTexture(Context context, String fn, Point screenSize, int Quality, int[] maxTextureWidth, int layout, float speedAnimation, boolean blur, int LevelGausse, int touch) {
        int xo = 0;
        int yo = 0;
        alpha = 0f;
        max = maxTextureWidth;
        error = false;
        mScreenSize = screenSize;
        indexLayer = layout;
        touchType = touch;
        quality = Quality;
            prepareFile(fn);
            if (fileType == FileType.VIDEO) {
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(filePath);
                    mediaPlayer.setLooping(true);
                    mediaPlayer.setVolume(0,0);
                    mediaPlayer.prepare();
                    mediaPlayer.seekTo(0);
                    xo = mediaPlayer.getVideoWidth();
                    yo = mediaPlayer.getVideoHeight();
                    mediaPlayer.start();
                 } catch (Exception e) {
                    error = true;
                    fileType = FileType.PICTURE;
                }
            }
            if (fileType == FileType.PICTURE) {
                Bitmap bmp = !error ? decodeSampledBitmapFromResource(filePath, max[0] / Quality, max[0] / Quality):
                             BitmapFactory.decodeResource(context.getResources(), R.drawable.home_wallpaper);
                mBitmap = Picture_Modified(bmp);
                if (blur) {
                    try {
                        doBlur(LevelGausse);
                    } catch (Exception ignored) {

                    }
                }
                xo = mBitmap.getWidth();
                yo = mBitmap.getHeight();
            }
            float d = (float) yo  / mScreenSize.y;
            int x = (int) ((float) xo / d);
            int y = mScreenSize.y;
            if (x < mScreenSize.x) {
                d = (float) xo / mScreenSize.x;
                y = (int) ((float) yo / d);
                x = mScreenSize.x;
            }
        max_speed = (x - mScreenSize.x) / (float) (screenCount - 1) / 20f;
        width = x;
        height = y;
        Random random = new Random();
        vector = random.nextInt(2) != 0;
        speed = ((width / (float) mScreenSize.x) -1) * speedAnimation;
        viewCreate();
    }

    void setParam(int touch, float speedAnimation){
        touchType = touch;
        speed = ((width / (float) mScreenSize.x) -1) * speedAnimation;
    }

    void texture() {
        GLES20.glGenTextures(1, mTexture, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + indexLayer);
        if (fileType == FileType.VIDEO) {
            int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTexture[0]);
            GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            mSurface = new SurfaceTexture(mTexture[0]);
            mediaPlayer.setSurface(new Surface(mSurface));
        }
        if (fileType == FileType.PICTURE) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
            mBitmap.recycle();
        }
        createProgram();
    }

    private void viewCreate(){
        ByteBuffer bb = ByteBuffer.allocateDirect(32);
        bb.order(ByteOrder.nativeOrder());
        uvBuffer = bb.asFloatBuffer();
        float[] uvs = new float[]{0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f};
        uvBuffer.put(uvs);
        uvBuffer.position(0);

        for(int i = 0; i < mtrxProjectionAndView.length; i++) {
            Matrix.orthoM(mtrxProjection, 0, 0f, mScreenSize.x, 0f, mScreenSize.y, 0, 50);
            Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0.0f);
            Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);
        }
    }

    private void createProgram(){
        String vs_Image = "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "attribute vec2 a_texCoord;" +
                "varying vec2 v_texCoord;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * vPosition;" +
                "  v_texCoord = a_texCoord;" +
                "}";
        vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vs_Image);

        String fs_Image = "";
        if (fileType == FileType.PICTURE) {
            fs_Image =
                    "precision mediump float;" +
                            "varying vec2 v_texCoord;" +
                            "uniform sampler2D s_texture;" +
                            "uniform float uAlpha;" +
                            "void main() {" +
                            "  vec4 textureColor = texture2D(s_texture, v_texCoord);" +
                            "  textureColor.a = uAlpha;" +
                            "  gl_FragColor = textureColor;" +
                            "}";
        }

        if (fileType == FileType.VIDEO) {
            fs_Image =
                    "#extension GL_OES_EGL_image_external : require\n" +
                            "precision mediump float;" +
                            "varying vec2 v_texCoord;" +
                            "uniform samplerExternalOES s_texture;" +
                            "uniform float uAlpha;" +
                            "void main() {" +
                            "  vec4 textureColor = texture2D(s_texture, v_texCoord);" +
                            "  textureColor.a = uAlpha;" +
                            "  gl_FragColor = textureColor;" +
                            "}";
        }
        fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs_Image);
        sp_Image = GLES20.glCreateProgram();
        GLES20.glAttachShader(sp_Image, vertexShader);
        GLES20.glAttachShader(sp_Image, fragmentShader);
        GLES20.glLinkProgram(sp_Image);
        GLES20.glUseProgram(sp_Image);

    }

    private int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    void SetupTriangle() {
        float[] vertices = new float[]
                {0.0f, (float) getHeight(), 0.0f,
                        0.0f, 0.0f, 0.0f,
                        (float) getWidth(), 0.0f, 0.0f,
                        (float) getWidth(),(float) getHeight(), 0.0f,
                };
        // The vertex buffer.
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);
    }

    void SetupCameraCenter(){
        float CenterScreen = ( (float) mScreenSize.x / 2) - (float) (getWidth() / 2);
        picturePosition = 0;
        Matrix.orthoM(mtrxProjection, 0, 0f, mScreenSize.x, 0f, mScreenSize.y, 0, 50);
        Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0.0f);
        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);
        matrixMove(CenterScreen);
    }
    private void matrixMove(float x) {
        Matrix.translateM(mtrxView, 0, x, 0, 0);
        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);
        picturePosition += x;
    }

    private void updateTexImage(){
        if (fileType == FileType.VIDEO) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) mSurface.updateTexImage();
        }
    }

    private Bitmap decodeSampledBitmapFromResource(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options,
                                      int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private Bitmap Picture_Modified(Bitmap in_bmp) {
        int xo = in_bmp.getWidth();
        int yo = in_bmp.getHeight();
        if (xo > max[0] || yo > max[0]) {
            float d = (float) yo / (float) max[0];
            int x = (int) ((float) xo / d);
            int y = max[0];
            if (x > max[0]) {
                d = (float) xo / (float) max[0];
                y = (int) ((float) yo / d);
                x = max[0];
            }
            return Bitmap.createScaledBitmap(in_bmp, x, y, true);
        } else return Bitmap.createBitmap(in_bmp);
    }

    void render(){
        int mPositionHandle = GLES20.glGetAttribLocation(sp_Image, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        int mTexCoordLoc = GLES20.glGetAttribLocation(sp_Image, "a_texCoord" );
        GLES20.glEnableVertexAttribArray ( mTexCoordLoc );
        GLES20.glVertexAttribPointer ( mTexCoordLoc, 2, GLES20.GL_FLOAT, false,	0, uvBuffer);
        int mtrxhandle = GLES20.glGetUniformLocation(sp_Image, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, mtrxProjectionAndView, 0);

        int mSamplerLoc = GLES20.glGetUniformLocation (sp_Image, "s_texture" );
        GLES20.glUniform1i (mSamplerLoc, indexLayer);

        int a = GLES20.glGetUniformLocation (sp_Image, "uAlpha" );
        GLES20.glUniform1f(a, getAlpha());
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordLoc);

        GLES20.glFinish();
    }

    private void motion() {
        if (speed > 0) {
            if (vector) {
                if ((picturePosition - speed) >= (mScreenSize.x - width)) {
                    matrixMove(-speed);
                } else {
                    vector = false;
                }
            } else {
                if ((picturePosition + speed) <= 0) {
                    matrixMove(speed);
                } else {
                    vector = true;
                }
            }
        }
    }
    private void touchMotion() {
        if (touchType>0) {
            if (motion_speed != 0) {
                if ((picturePosition + motion_speed > (mScreenSize.x + ((float) width / 100)) - width)
                        && ((picturePosition + motion_speed) < (0 - ((float) width / 100)))) {
                    matrixMove(motion_speed);
                    if (motion_speed > 0) {
                        motion_speed = (float) (motion_speed - (Math.sqrt(Math.abs(motion_speed)) / screenCount));
                        if (motion_speed < speed) motion_speed = 0;
                    } else {
                        motion_speed = (float) (motion_speed + (Math.sqrt(Math.abs(motion_speed)) / screenCount));
                        if (motion_speed > -speed) motion_speed = 0;
                    }
                } else  motion_speed = 0;
            }
        }
    }

    int getWidth(){
        return width;
    }
    int getHeight(){
        return height;
    }
    void release(){
        if (mediaPlayer!=null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        fileType = FileType.NONE;
        GLES20.glDeleteTextures(1, mTexture, 0);
        GLES20.glDetachShader(sp_Image, vertexShader);
        GLES20.glDetachShader(sp_Image, fragmentShader);
        GLES20.glDeleteShader(fragmentShader);
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteProgram(sp_Image);

    }
    void setSound(float level) {
        if (fileType == FileType.VIDEO) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.setVolume(level, level);
        }
    }
    void onPause() {
        if (fileType == FileType.VIDEO) {
            if (mediaPlayer != null) mediaPlayer.pause();
        }
    }
    void onResume() {
        if (fileType == FileType.VIDEO) {
            if (mediaPlayer != null) mediaPlayer.start();
        }
    }
    private void prepareFile(String fn){
        if (new File(fn).exists()){
            filePath = fn;
            if (!filePath.endsWith(".mp4")) {
                fileType = FileType.PICTURE;
            } else {
                fileType = FileType.VIDEO;
            }
        } else {
            fileType = FileType.PICTURE;
            error = true;
        }
    }
    void update(){
        if (alpha < 1.0f) alpha +=0.02f;
        if (alpha > 1.0f) alpha = 1f;
        updateTexImage();
        if (motion_speed != 0) {touchMotion();} else {motion();}
    }
    float getAlpha(){
        return alpha;
    }
    void setMoutionSpeed(float speed){
        motion_speed = speed;
    }
    float getMaxSpeed(){
        return max_speed;
    }
    private void doBlur(int radius) {

        Bitmap bitmap = Bitmap.createScaledBitmap(mBitmap, mBitmap.getWidth()/quality+1, mBitmap.getHeight()/quality+1, true);

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int [] pix = new int[w * h];

        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int[] g = new int[wh];
        int[] b = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int[] vmin = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int[] dv = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        int[] r = new int[wh];
        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        mBitmap = bitmap.copy(bitmap.getConfig(), true);
        bitmap.recycle();
    }
}


public class ImageWallpaperSCRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private ImageWallpaperSCMediaTexture[] mediaTextures = new ImageWallpaperSCMediaTexture[2];
    private int ActiveLayout = 0;
    private int[] Action = new int[] {0, 0};           //Текущее действие для текстуры
    private VelocityTracker j;
    private float offsetX = 0f;
    private final Random random = new Random();
    long TimeChange;
    private Point mScreenSize = new Point();
    private ArrayList<File> files = new ArrayList<>();
    private final String[] ext = {".jpg", ".png", ".jpeg", ".bmp", ".gif", ".mp4"};
    private Context mContext;
    private String Path = "/";
    int Timer = 20;
    private long TimeSet = 20 * 60000;
    private float SetSpeed = 0.2f;
    private int Quality = 2;
    private int LevelGausse = 2;
    private boolean blur = false;
    private int touch = 2;
    private boolean VolumeEnable = false;
    private int[] max = new int[1];
    private String currentPathList; //Это нужно что бы при смене папки с файлами, при загрузке текстуры листинг обновить

    ImageWallpaperSCRender(Context context) {
        mContext = context;
        loadParam(mContext);
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        assert windowManager != null;
        Display display = windowManager.getDefaultDisplay();
        display.getRealSize(mScreenSize);
        TimeChange = SystemClock.uptimeMillis(); //Время смены обновляем на текущее
    }

    void onPause(){
        for (int i = 0; i<2; i++)
            if (mediaTextures[i]!=null) mediaTextures[i].onPause();
    }

    void onResume(){
        for (int i = 0; i<2; i++)
            if (mediaTextures[i]!=null) mediaTextures[i].onResume();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, max, 0); //put the maximum texture size in the array.

        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        ActiveLayout = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        TimeSet = Timer * 60000;
    }

    private void ProgramAnimate(int layer) {
        switch (Action[layer]){
            case 0: //Загрузка новой текстуры
                Action[layer]++; //Переходим на слудующее действие, т.е. ждем загрузки
                LoadTexture loading = new LoadTexture(layer);
                loading.executeOnExecutor(THREAD_POOL_EXECUTOR);
                break;
            case 1: //идет загузка текстуры
                break;
            case 2:
                if (mediaTextures[layer]!=null) mediaTextures[layer].texture();
                mediaTextures[layer].SetupTriangle();
                mediaTextures[layer].SetupCameraCenter();
                Action[layer]++;
            case 3: //текстура загружена, слой подготовлен
                if (mediaTextures[layer]!=null) {
                    mediaTextures[layer].update(); //расчет точек положения и прочего
                    mediaTextures[layer].render();
                }
                break;
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        ProgramAnimate(ActiveLayout);

        int InactiveLayout = ActiveLayout==0?1:0;
        if (Timer > 0 && (SystemClock.uptimeMillis() - TimeChange) >= TimeSet) { //Пришло время менять картинку
            ProgramAnimate(InactiveLayout);                         //Запускаем загрузку невидимого слоя
        }

        //Поскольку после загрузки невидимого слоя время смены обновится, нужно дальше отрисовывать его по action 3
        if (Action[InactiveLayout]>1){
            ProgramAnimate(InactiveLayout);
            if (VolumeEnable) {
                if (mediaTextures[InactiveLayout]!=null)
                mediaTextures[InactiveLayout].setSound(mediaTextures[InactiveLayout].getAlpha());
                if (mediaTextures[InactiveLayout]!=null && mediaTextures[ActiveLayout]!=null)
                mediaTextures[ActiveLayout].setSound(1f - mediaTextures[InactiveLayout].getAlpha());
            }
            //Когда альфа невидимого слоя стала = 1, т.е. он стал полностью видимым, то нужно сменить слои, и удалить старый кадр
            if (mediaTextures[InactiveLayout]!=null)
            if (mediaTextures[InactiveLayout].getAlpha() >= 1f) {
                if (mediaTextures[ActiveLayout]!=null) {
                    mediaTextures[ActiveLayout].release();
                    mediaTextures[ActiveLayout] = null;
                }
                Action[ActiveLayout] = 0;
                ActiveLayout = InactiveLayout;
            }
        }

    }

        void onOffsetsChanged(float xOffset) {
            if (touch == 2) {
                for (int i = 0; i < 2; i++)
                    if (mediaTextures[i]!=null) mediaTextures[i].setMoutionSpeed((mediaTextures[i].getWidth() - mScreenSize.x) * (offsetX - xOffset));
                offsetX = xOffset;
            }
        }

        void onTouchEvent(MotionEvent motionEvent) {
            if (touch == 1) {
                int action = motionEvent.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        if (j == null) {
                            j = VelocityTracker.obtain();
                        } else {
                            j.clear();
                        }
                        j.addMovement(motionEvent);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        j.addMovement(motionEvent);
                        for (int i = 0; i < 2; i++) {
                            if (mediaTextures[i]!=null) {
                                j.computeCurrentVelocity((int) mediaTextures[i].getMaxSpeed(), mediaTextures[i].getMaxSpeed());
                                mediaTextures[i].setMoutionSpeed(j.getXVelocity());
                            }
                        }
                        break;
                }
            }
        }


    @Override
    public void onFrameAvailable(SurfaceTexture surface) {
    }


    @SuppressLint("StaticFieldLeak")
    private class LoadTexture extends AsyncTask<Integer, Void, Void> {
        int currentTexture;

        LoadTexture(int layer) {
            currentTexture = layer;
        }

        private ArrayList<File> listFilesWithFolders(String folder) {
            ArrayList<File> files = new ArrayList<>();
            try {
                File dir = new File(folder);
                for (File file : Objects.requireNonNull(dir.listFiles()))
                    if (!file.isDirectory()) {
                        for (String ext_t : ext) {
                            if (file.getName().toUpperCase().endsWith(ext_t.toUpperCase())) {
                                files.add(file);
                                break;
                            }
                        }
                    }
            } catch (Exception ignored) {

            }
            currentPathList = folder;
            return files;
        }

        @Override
        protected Void doInBackground(Integer... params) {
            if (files.size() == 0) files = listFilesWithFolders(Path);
            if (!currentPathList.equals(Path)) files = listFilesWithFolders(Path);
            String FileName;
            int fileNum;
            boolean fileExists;
            if (files.size()>0) {
                do {
                    fileNum = random.nextInt(files.size());
                    FileName = Path + files.get(fileNum).getName();
                    files.remove(fileNum);
                    fileExists = new File(FileName).exists();
                } while (!fileExists && files.size() > 0);
            } else FileName = "";
            if (mediaTextures[currentTexture] != null) {
                mediaTextures[currentTexture].release();
                mediaTextures[currentTexture] = null;
            }
            mediaTextures[currentTexture] = new ImageWallpaperSCMediaTexture(mContext, FileName, mScreenSize, Quality, max, currentTexture, SetSpeed, blur, LevelGausse, touch);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            TimeChange = SystemClock.uptimeMillis(); //Время смены обновляем на текущее
            Action[currentTexture]++; //Ставим следующее действие
        }

    }

    void loadParam(Context mContext) {
        Path = PreferenceManager.getDefaultSharedPreferences(mContext).getString("directory","/");
        blur = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("blur",false);
        Timer = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString("duration","20"));
        TimeSet = Timer * 60000;
        touch = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString("touch","2"));
        SetSpeed = Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(mContext).getString("speed","0.5"));
        Quality = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString("quality","2"));
        LevelGausse = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString("blur_level","5"));
        VolumeEnable = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("sound",false);

        for (int i=0; i<2; i++) if (mediaTextures[i] != null) mediaTextures[i].setParam(touch, SetSpeed);
    }



}
