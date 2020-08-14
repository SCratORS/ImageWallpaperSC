package com.scrat.imagewallpapersc;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.VelocityTracker;
import android.view.WindowManager;

import androidx.documentfile.provider.DocumentFile;

import java.io.FileDescriptor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static android.view.Surface.*;

class ImageWallpaperSCMediaTexture {
    protected enum FileType {
        PICTURE, VIDEO, NONE
    }
    boolean ErrorLoad;
    private final int[] max;
    private FileType fileType = FileType.NONE;
    private MediaPlayer mediaPlayer;
    private final int width;
    private final int height;
    private int width_matrix;
    private int height_matrix;
    private SurfaceTexture mSurface;
    private int sp_Image;
    private float alpha;
    private Bitmap mBitmap;
    private final int[] mTexture = new int[1];

    private float picturePosition_x;
    private float picturePosition_y;

    private float max_speed;
    private float motion_speed_x;
    private float motion_speed_y;
    private float tilt_val_x = 0;
    private float tilt_val_y = 0;
    private int tilt;
    private boolean vector_x;
    private boolean vector_y;
    private float speed_x;
    private float speed_y;
    private final int screenCount = 5;
    private Point mScreenSize;
    private final int indexLayer;
    private int touchType;

    private FloatBuffer uvBuffer;
    private final float[] matrixProjection = new float[16];
    private final float[] matrixView = new float[16];
    private final float[] matrixProjectionAndView = new float[16];
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    private final short[] indices = new short[]{0, 1, 2, 0, 2, 3};
    private int fragmentShader;
    private int vertexShader;
    private float mSpeedAnimation;
    private boolean centeringCamera;
    private int mRotation;
    private int mScale;

    ImageWallpaperSCMediaTexture(Context context, Uri filePath, Point screenSize, int Quality, int[] maxTextureWidth, int layout, float speedAnimation, int touch, int tilt_sh, int scale) {
        int xo = 0;
        int yo = 0;
        mSpeedAnimation = speedAnimation;
        tilt = tilt_sh;
        alpha = 0f;
        max = maxTextureWidth;
        mScale = scale;
        boolean error = false;
        mScreenSize = screenSize;
        indexLayer = layout;
        touchType = touch;
        ErrorLoad = false;
        centeringCamera = true;

        String mime = context.getContentResolver().getType(filePath);
        if (mime == null || mime.startsWith("image")) {
            fileType = FileType.PICTURE;
        } else if (mime.startsWith("video"))
            fileType = FileType.VIDEO;

            if (fileType == FileType.VIDEO) {
               try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(context, filePath);
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
                Bitmap bmp;
                bmp = !error ? decodeSampledBitmapFromResource(context, filePath, max[0] / Quality, max[0] / Quality):
                        BitmapFactory.decodeResource(context.getResources(), R.drawable.home_wallpaper);
                try {
                    mBitmap = Picture_Modified(bmp); //<-- тут выскакивает null pointer на in_bmp.getWidth(); Вероятно связано с невозможностью загрузить картинку или еще какой гадостью
                    xo = mBitmap.getWidth();
                    yo = mBitmap.getHeight();
                } catch (Exception e) {
                    ErrorLoad = true;
                }
            }
        //записываем размер матрицы равной размеру картинки
        width = xo;
        height = yo;

        if (!ErrorLoad) {
            Random random = new Random();
            vector_x = random.nextInt(2) != 0;
            vector_y = random.nextInt(2) != 0;
            viewCreate();
        }
    }

    void setParam(int touch, float speedAnimation, int tilt_sh, int scale){
        touchType = touch;
        tilt = tilt_sh;
        mSpeedAnimation = speedAnimation;
        mScale = scale;
        speed_x = ((width / (float) mScreenSize.x) -1) * mSpeedAnimation;
        speed_y = ((height / (float) mScreenSize.y) -1) * mSpeedAnimation;
    }

    void setCentering(boolean center){
        centeringCamera = center;
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
    }

    void viewRecreate(Point screenSize, int rotation){
        centeringCamera = !centeringCamera && (mScreenSize.x != screenSize.x || mScreenSize.y != screenSize.y); //Если изменился размер картинки
        centeringCamera = !centeringCamera && mRotation != rotation; //Если изменилась ориентация
        mScreenSize = screenSize;
        mRotation = rotation;
        SetupTriangle();
        SetupCameraCenter();
    }

    private void createProgram(){
        String vs_Image = "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "attribute vec2 a_texCord;" +
                "varying vec2 v_texCord;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * vPosition;" +
                "  v_texCord = a_texCord;" +
                "}";
        vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vs_Image);

        String fs_Image = "";
        if (fileType == FileType.PICTURE) {
            fs_Image =
                    "precision mediump float;" +
                            "varying vec2 v_texCord;" +
                            "uniform sampler2D s_texture;" +
                            "uniform float uAlpha;" +
                            "void main() {" +
                            "  vec4 textureColor = texture2D(s_texture, v_texCord);" +
                            "  textureColor.a = uAlpha;" +
                            "  gl_FragColor = textureColor;" +
                            "}";
        }

        if (fileType == FileType.VIDEO) {
            fs_Image =
                    "#extension GL_OES_EGL_image_external : require\n" +
                            "precision mediump float;" +
                            "varying vec2 v_texCord;" +
                            "uniform samplerExternalOES s_texture;" +
                            "uniform float uAlpha;" +
                            "void main() {" +
                            "  vec4 textureColor = texture2D(s_texture, v_texCord);" +
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
        //скалим размер матрицы под размер экрана
        Point nScreen = new Point();

        nScreen.x = (int) (mScreenSize.x * (mScale / 100f));
        nScreen.y = (int) (mScreenSize.y * (mScale / 100f));

        float d = (float) height  / nScreen.y;
        int x = (int) ((float) width / d);
        int y = nScreen.y;

        if (x < mScreenSize.x) {
            d = (float) width / nScreen.x;
            y = (int) ((float) height / d);
            x = nScreen.x;
        }

        float m_s_x = (x - mScreenSize.x) / (float) (screenCount - 1) / 10f;
        float m_s_y = (y - mScreenSize.y) / (float) (screenCount - 1) / 10f;
        max_speed = (m_s_x + m_s_y) / 2;

        speed_x = ((x / (float) mScreenSize.x) -1) * mSpeedAnimation;


        speed_y = ((y / (float) mScreenSize.y) -1) * mSpeedAnimation;

        centeringCamera = !centeringCamera && (width_matrix != x || height_matrix != y); //Если изменился масштаб

        width_matrix = x;
        height_matrix = y;

        float[] vertices = new float[]
                {0.0f, (float)  y, 0.0f,
                        0.0f, 0.0f, 0.0f,
                        (float) x, 0.0f, 0.0f,
                        (float) x,(float)  y, 0.0f,
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
        float CenterScreen_x = ( (float) mScreenSize.x / 2) - (float) (width_matrix / 2);
        float CenterScreen_y = ( (float) mScreenSize.y / 2) - (float) (height_matrix / 2);
        float currentPosition_x = picturePosition_x; //Запоминаем позицию которая была, если блок-разблок
        float currentPosition_y = picturePosition_y;
        picturePosition_x = 0; //Обнуляем
        picturePosition_y = 0; //Обнуляем
        Matrix.orthoM(matrixProjection, 0, 0f, mScreenSize.x, 0f, mScreenSize.y, 0, 50);
        Matrix.setLookAtM(matrixView, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0.0f);
        Matrix.multiplyMM(matrixProjectionAndView, 0, matrixProjection, 0, matrixView, 0);
        if (centeringCamera) {
            matrixMove(CenterScreen_x, CenterScreen_y);
        } else {
            matrixMove(currentPosition_x, currentPosition_y);
        }//Если новая картинка, то центрируем, если нет, то ставим её в то место на котором остановились
        centeringCamera = false; //Указываем что больше её центрировать не надо
    }
    private void matrixMove(float x, float y) {
        Matrix.translateM(matrixView, 0, x, y, 0);
        Matrix.multiplyMM(matrixProjectionAndView, 0, matrixProjection, 0, matrixView, 0);
        picturePosition_x += x;
        picturePosition_y += y;
    }

    private void updateTexImage(){
        if (fileType == FileType.VIDEO) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) mSurface.updateTexImage();
        }
    }


    private Bitmap decodeSampledBitmapFromResource(Context context, Uri uri, int reqWidth, int reqHeight) {
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            ParcelFileDescriptor parcelFileDescriptor =
                    context.getContentResolver().openFileDescriptor(uri, "r"); //Error
            FileDescriptor fileDescriptor = null;
            if (parcelFileDescriptor != null) {
                fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            }
            BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
            }
            return image;
        } catch (Exception e) {
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.home_wallpaper);
        }
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
        int mTexCordLoc = GLES20.glGetAttribLocation(sp_Image, "a_texCord" );
        GLES20.glEnableVertexAttribArray ( mTexCordLoc );
        GLES20.glVertexAttribPointer ( mTexCordLoc, 2, GLES20.GL_FLOAT, false,	0, uvBuffer);
        int matrixHandle = GLES20.glGetUniformLocation(sp_Image, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, matrixProjectionAndView, 0);

        int mSamplerLoc = GLES20.glGetUniformLocation (sp_Image, "s_texture" );
        GLES20.glUniform1i (mSamplerLoc, indexLayer);

        int a = GLES20.glGetUniformLocation (sp_Image, "uAlpha" );
        GLES20.glUniform1f(a, getAlpha());
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCordLoc);

        GLES20.glFinish();
    }

    private void tilt_motion(){
        float value_x = tilt_val_x * tilt;
        float value_y = tilt_val_y * tilt;
        float r_value_x = 0;
        float r_value_y = 0;
        if ((picturePosition_x + value_x >= mScreenSize.x - width_matrix) && (picturePosition_x + value_x <= 0)) {
            r_value_x = value_x;
        }
        if ((picturePosition_y + value_y >= mScreenSize.y - height_matrix) && (picturePosition_y + value_y <= 0)) {
            r_value_y = value_y;
        }
        if (r_value_x != 0 || r_value_y != 0 ) matrixMove(r_value_x,r_value_y);
    }

    private void motion() {
        float r_speed_x, r_speed_y;
        r_speed_x = 0;
        r_speed_y = 0;
        if (speed_x > 0) {
            if (vector_x) {
                if ((picturePosition_x - speed_x) >= (mScreenSize.x - width_matrix)) {
                    r_speed_x = -speed_x;
                } else {
                    vector_x = false;
                }
            } else {
                if ((picturePosition_x + speed_x) <= 0) {
                    r_speed_x = speed_x;
                } else {
                    vector_x = true;
                }
            }
        }
        if (speed_y > 0) {
            if (vector_y) {
                if ((picturePosition_y - speed_y) >= (mScreenSize.y - height_matrix)) {
                    r_speed_y = -speed_y;
                } else { vector_y = false; }
            } else {
                if ((picturePosition_y + speed_y) <= 0) {
                    r_speed_y = speed_y;
                } else { vector_y = true; }
            }
        }
        if (speed_x > 0 || speed_y > 0)  matrixMove(r_speed_x, r_speed_y);
    }
    private void touchMotion() {
        float r_x = 0f;
        float r_y = 0f;
        if (touchType>0) {
            if (motion_speed_x != 0) {
                if ((picturePosition_x + motion_speed_x > (mScreenSize.x + ((float) width_matrix / 100)) - width_matrix)
                        && ((picturePosition_x + motion_speed_x) < (0 - ((float) width_matrix / 100)))) {
                    r_x = motion_speed_x;
                    if (motion_speed_x > 0) {
                        motion_speed_x = (float) (motion_speed_x - (Math.sqrt(Math.abs(motion_speed_x)) / screenCount));
                        if (motion_speed_x < speed_x) motion_speed_x = 0;
                    } else {
                        motion_speed_x = (float) (motion_speed_x + (Math.sqrt(Math.abs(motion_speed_x)) / screenCount));
                        if (motion_speed_x > -speed_x) motion_speed_x = 0;
                    }
                } else  motion_speed_x = 0;
            }
            if (motion_speed_y != 0) {
                if ((picturePosition_y + motion_speed_y > (mScreenSize.y + ((float) height_matrix / 100)) - height_matrix)
                        && ((picturePosition_y + motion_speed_y) < (0 - ((float) height_matrix / 100)))) {
                    r_y = motion_speed_y;
                    if (motion_speed_y > 0) {
                        motion_speed_y = (float) (motion_speed_y - (Math.sqrt(Math.abs(motion_speed_y)) / screenCount));
                        if (motion_speed_y < speed_y) motion_speed_y = 0;
                    } else {
                        motion_speed_y = (float) (motion_speed_y + (Math.sqrt(Math.abs(motion_speed_y)) / screenCount));
                        if (motion_speed_y > -speed_y) motion_speed_y = 0;
                    }
                } else  motion_speed_y = 0;
            }
            if (r_x != 0 || r_y != 0) matrixMove(r_x, r_y);
        }
    }

    int getWidth(){
        return width_matrix;
    }
    int getHeight(){
        return height_matrix;
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
            if (mediaPlayer != null) {
                    mediaPlayer.start();
            }
        }
    }

    void update(){
        if (alpha < 1.0f) alpha +=0.02f;
        if (alpha > 1.0f) alpha = 1f;
        updateTexImage();
        if (motion_speed_x != 0 || motion_speed_y != 0) {touchMotion();} else {motion();}
        tilt_motion();
    }

    float getAlpha(){
        return alpha;
    }
    void setMotionSpeed(float speed_x, float speed_y){
        motion_speed_x = speed_x;
        motion_speed_y = -speed_y;
    }
    void setTiltValue(float s0, float s1){
        switch (mRotation) {
            case ROTATION_90:
                tilt_val_x = s0;
                tilt_val_y = s1;
                break;
            case ROTATION_180:
                tilt_val_x = -s1;
                tilt_val_y = s0;
                break;
            case ROTATION_270:
                tilt_val_x = -s0;
                tilt_val_y = -s1;
                break;
            default:
                tilt_val_x = s1;
                tilt_val_y = -s0;
                break;
        }

    }
    float getMaxSpeed(){
        return max_speed;
    }
}


class ImageWallpaperSCRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private final ImageWallpaperSCMediaTexture[] mediaTextures = new ImageWallpaperSCMediaTexture[2];
    private int ActiveLayout = 0;
    private final int[] Action = new int[] {0, 0};           //Текущее действие для текстуры
    private VelocityTracker j;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private final Random random = new Random();
    private long TimeChange;
    private final Point mScreenSize = new Point();
    private ArrayList<DocumentFile> files = new ArrayList<>();
    private final Context mContext;
    private String Path = "";
    private Set<String> PathSet;
    private int Mode = 0;
    private int Timer = 20;
    private long TimeSet = 20 * 60000;
    private float SetSpeed = 0.2f;
    private int Quality = 2;
    private int tilt = 0;
    private boolean double_tap = true;
    private int scale = 100;
    private int touch = 2;
    private boolean VolumeEnable = false;
    private final int[] max = new int[1];
    private String currentPathList; //Это нужно что бы при смене папки с файлами, при загрузке текстуры листинг обновить
    private BroadcastReceiver receiver;

    ImageWallpaperSCRender(Context context) {
        mContext = context;
        loadParam(mContext);
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        assert windowManager != null;
        Display display = windowManager.getDefaultDisplay();
        display.getRealSize(mScreenSize);
        TimeChange = SystemClock.elapsedRealtime(); //Время смены обновляем на текущее
        IntentFilter filter = new IntentFilter("com.scrat.imagewallpapersc.UpdateDBForSaveFolder");
        receiver = new UpdateReceiver();
        context.registerReceiver(receiver, filter);
    }

    void onPause(){
        for (int i = 0; i<2; i++)
            if (mediaTextures[i]!=null) mediaTextures[i].onPause();
    }

    void onResume(){
            if (mediaTextures[ActiveLayout]!=null) mediaTextures[ActiveLayout].onResume();
    }

    void onDestroy(){
        mContext.unregisterReceiver(receiver);
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
        Point newScreenSize = new Point();
        newScreenSize.x = width;
        newScreenSize.y = height;
        int mRotate;
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        assert windowManager != null;
        mRotate = windowManager.getDefaultDisplay().getRotation();
        GLES20.glViewport(0, 0, newScreenSize.x, newScreenSize.y);
        for (int i = 0; i<2; i++)
            if (mediaTextures[i]!=null) mediaTextures[i].viewRecreate(newScreenSize, mRotate);
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
                if (mediaTextures[layer]!=null) {
                    mediaTextures[layer].texture();
                    mediaTextures[layer].SetupTriangle();
                    mediaTextures[layer].setCentering(true);
                    mediaTextures[layer].SetupCameraCenter();
                }
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
        if (Timer > 0 && (SystemClock.elapsedRealtime() - TimeChange) >= TimeSet) { //Пришло время менять картинку
            if ((Mode == 1 && PathSet.size()>1) || Mode == 2)  ProgramAnimate(InactiveLayout);                         //Запускаем загрузку невидимого слоя
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

        void onOffsetsChanged(float xOffset, float yOffset) {
            if (touch == 2) {
                for (int i = 0; i < 2; i++)
                    if (mediaTextures[i]!=null) mediaTextures[i].setMotionSpeed((mediaTextures[i].getWidth() - mScreenSize.x) * (offsetX - xOffset), (mediaTextures[i].getHeight() - mScreenSize.y) * (offsetY - yOffset));
                offsetX = xOffset;
                offsetY = yOffset;
            }
        }

        void Sensor_Event(float s0, float s1) {
            if (tilt != 0) {
                for (int i = 0; i < 2; i++)
                    if (mediaTextures[i]!=null) mediaTextures[i].setTiltValue(s0, s1);
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
                                mediaTextures[i].setMotionSpeed(j.getXVelocity(), j.getYVelocity());
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
        final int currentTexture;

        LoadTexture(int layer) {
            currentTexture = layer;
        }

        private ArrayList<DocumentFile> listFilesWithFolders(Uri folder) { //NullPointerException
            ArrayList<DocumentFile> files = new ArrayList<>();
            if (folder.isAbsolute()) {
                DocumentFile documentsTree = DocumentFile.fromTreeUri(mContext, folder);
                if (documentsTree != null) {
                    files = new ArrayList<>(Arrays.asList(documentsTree.listFiles()));
                }
            }
            currentPathList = folder.toString();
            return files;
        }

        private ArrayList<DocumentFile> listFilesWithMulti(Set<String> pathSet) { //NullPointerException
            ArrayList<DocumentFile> documentFiles = new ArrayList<>();
            for(String path : pathSet){
                Uri file = Uri.parse(path);
                if (file.isAbsolute()) {
                    documentFiles.add(DocumentFile.fromSingleUri(mContext, file));
                }
            }
            currentPathList = pathSet.toString();
            return documentFiles;
        }

        @Override
        protected Void doInBackground(Integer... params) {
            if (files.size() == 0 || !currentPathList.equals(Path)) {
                int circleLoad = 10000;
                do {
                    circleLoad--;
                    if (Mode == 2) files = listFilesWithFolders(Uri.parse(Path));
                    if (Mode == 1) files = listFilesWithMulti(PathSet);
                } while (files.size() == 0 && circleLoad > 0);
            }
            Uri FileName = Uri.parse("");
            int fileNum;
            boolean fileCorrect = false;
            if (files.size() > 0) {
                do {
                    fileNum = random.nextInt(files.size());
                    DocumentFile file = files.get(fileNum);
                    files.remove(fileNum);
                    if (!file.isDirectory()) {
                        String mime = mContext.getContentResolver().getType(file.getUri());
                        if (mime!=null && (mime.startsWith("image") || mime.startsWith("video"))) {
                            fileCorrect = true;
                            FileName = file.getUri();
                        }
                    }
                }
                while (!fileCorrect && files.size() > 0);
            }
            if (mediaTextures[currentTexture] != null) {
                mediaTextures[currentTexture].release();
                mediaTextures[currentTexture] = null;
            }
            mediaTextures[currentTexture] = new ImageWallpaperSCMediaTexture(mContext, FileName, mScreenSize, Quality, max, currentTexture, SetSpeed, touch, tilt, scale);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (!mediaTextures[currentTexture].ErrorLoad) {
                TimeChange = SystemClock.elapsedRealtime(); //Время смены обновляем на текущее
                Action[currentTexture]++; //Ставим следующее действие
            } else {
                Action[currentTexture] = 0;
            }
        }
    }

    boolean getDoubleTapSetting(){
        return double_tap;
    }
    int getTiltSetting(){
        return tilt;
    }

    private int StrToIntDef(String s) {
        int result;
        try {
            result = Integer.parseInt(s);
        } catch(Exception e) {
            result = 0;
        }
        return result;
    }

    void change(){
        TimeChange = SystemClock.elapsedRealtime(); //Что бы 2 раза не сменилось случайно, по 2 тапу и по времени
        int layer = ActiveLayout==0?1:0;
        if (Action[layer]==0) ProgramAnimate(layer);
    }

    void loadParam(Context mContext) {
        SharedPreferences settings = mContext.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        if(settings.contains("mode")) {
            Mode = settings.getInt("mode", 0);
            if(Mode == 2 && settings.contains("directory")) {
                Path = settings.getString("directory", "");
            }
            if(Mode == 1 && settings.contains("multi")) {
                PathSet = settings.getStringSet("multi", null);
                if (PathSet!=null) Path = PathSet.toString(); else Path = "";
            }
        }

        double_tap = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("double_tap",true);
        Timer = StrToIntDef(PreferenceManager.getDefaultSharedPreferences(mContext).getString("duration","20"));
        TimeSet = Timer * 60000;
        touch = Integer.parseInt(Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(mContext).getString("touch", "2")));
        tilt = Integer.parseInt(Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(mContext).getString("tilt", "0")));
        SetSpeed = Float.parseFloat(Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(mContext).getString("speed", "0.5")));
        Quality = Integer.parseInt(Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(mContext).getString("quality", "2")));
        scale = Integer.parseInt(Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(mContext).getString("scale", "100")));
        VolumeEnable = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("sound",false);

        for (int i=0; i<2; i++) if (mediaTextures[i] != null) {
            mediaTextures[i].setParam(touch, SetSpeed, tilt, scale);
        }
    }

    private class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadParam(context);
            change();
        }
    }

}
