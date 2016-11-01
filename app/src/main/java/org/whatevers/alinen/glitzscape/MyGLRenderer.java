package org.whatevers.alinen.glitzscape;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by alinen on 10/19/2016.
 */

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private float mLastTime = 0f;
    private final float[] mPMatrix = new float[16];
    private final float WORLD_SIZE = 10.0f;
    private HexBoard mBoard;
    private AssetManager mAssetManager;
    private int mTextureId;
    private Bitmap mBitmap;

    public MyGLRenderer(AssetManager mgr)
    {
        mAssetManager = mgr;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {
        GLES20.glClearColor(1.0f,0.0f,0.0f,1.0f);
        mBoard = new HexBoard(2.0f, WORLD_SIZE, 0.2f);
        mBoard.initBoard();

        // load texture
        mBitmap = loadTextureResource(mAssetManager, "stripesTest7.png");
        mTextureId = initTexture(mBitmap, false);
    }

    @Override
    public void onDrawFrame(GL10 unused)
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        float time = SystemClock.uptimeMillis() * 0.001f; // mm to s
        float dt = time - mLastTime;
        mBoard.draw(time, mPMatrix, mTextureId);
        mLastTime = time;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height)
    {
        GLES20.glViewport(0,0,width, height);
        Matrix.orthoM(mPMatrix, 0, -WORLD_SIZE, WORLD_SIZE, -WORLD_SIZE, WORLD_SIZE, 0.1f, 10.0f);
    }

    static int initTexture(Bitmap bitmap, boolean mipmaps)
    {
        //int[] pixels = new int[bitmap.getWidth()] = bitmap.getPixels();
        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
       /*
        //gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true); // asn: do I need this?
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                bitmap.getWidth(), bitmap.getHeight(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bitmap.get);
        if (mipmaps)
        {
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST_MIPMAP_LINEAR);
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        }
        else
        {
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        */
        return textureIds[0];
    }

    /*
    function initTexture()
    {
        var canvas = document.createElement("canvas");
        canvas.width = 256;
        canvas.height = 256;

        var ctx = canvas.getContext('2d');
        ctx.createImageData(canvas.width, canvas.height);
        ctx.fillRect(0,0,canvas.width, canvas.height);

        var cmIdx = Math.floor(Math.random() * colormapList.length);
        randomStripes(canvas, colormapList[cmIdx], 32);
        subVertical(canvas, 16);
        smoothBox(canvas, 2);

        backgroundTex = gl.createTexture();
        backgroundTex.image = ctx.getImageData(0, 0, canvas.width, canvas.height);

        handleLoadedTexture(backgroundTex, false);
    }*/

    static int initTextureFromFile(AssetManager mgr, String filename, boolean mipmaps)
    {
        Bitmap bitmap = loadTextureResource(mgr, filename);
        return initTexture(bitmap, mipmaps);
    }

    static Bitmap loadTextureResource(AssetManager mgr, String filename)
    {
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = mgr.open(filename);
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            bitmap = BitmapFactory.decodeStream(is, null, options);
        }
        catch (final IOException e) {
            bitmap = null;
            Log.e("GLITZ", "FAILED to get bitmap asset "+filename+" "+e.getMessage());
        }
        finally
        {
            if (is != null) {
                try { is.close(); }
                catch (IOException e) {}
            }
        }
        return bitmap;
    }
}
