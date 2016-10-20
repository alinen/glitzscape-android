package org.whatevers.alinen.glitzscape;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

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

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config)
    {
        GLES20.glClearColor(1.0f,0.0f,0.0f,1.0f);
        mBoard = new HexBoard(2.0f, WORLD_SIZE, 0.2f);
        mBoard.initBoard();
    }

    @Override
    public void onDrawFrame(GL10 unused)
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        float time = SystemClock.uptimeMillis() * 0.001f; // mm to s
        float dt = time - mLastTime;
        mBoard.draw(dt, mPMatrix);
        mLastTime = time;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height)
    {
        GLES20.glViewport(0,0,width, height);
        Matrix.orthoM(mPMatrix, 0, -WORLD_SIZE, WORLD_SIZE, -WORLD_SIZE, WORLD_SIZE, 0.1f, 10.0f);
    }



}
