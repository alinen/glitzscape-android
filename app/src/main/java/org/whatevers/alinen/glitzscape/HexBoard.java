package org.whatevers.alinen.glitzscape;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import android.content.res.AssetManager;
import android.util.Log;
import android.opengl.Matrix;
import android.opengl.GLES20;

/**
 * Created by alinen on 10/19/2016.
 */

public class HexBoard {

    protected static final ArrayList<float[]> mvMatrixStack = new ArrayList<float[]>();
    protected static float[] mvMatrix = new float[16]; // move to base class eventually
    protected boolean mEnabled = true; // todo: move to base class
    protected Vector3D mTranslation;
    protected float mRotationZ;
    protected float mUniformScale;
    protected int mShader;
    protected int mPositionHandle;
    protected int mUVHandle;
    protected int mColorHandle;
    protected int mMVMatHandle;
    protected int mPMatHandle;
    protected int mTimeHandle;
    protected int mSamplerHandle;
    protected int mPrimitive;

    private final String vertexShaderCode =
            "attribute vec3 aPosition;\n" +
            "attribute vec2 aTextureCoord;\n" +
            "attribute vec4 aColor;\n" +
            "uniform mat4 uMVMatrix;\n" +
            "uniform mat4 uPMatrix;\n" +
            "varying vec2 vTextureCoord;\n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            "  gl_Position = uPMatrix * uMVMatrix * vec4(aPosition,1.0);\n" +
            "  vTextureCoord = aTextureCoord;\n" +
            "  vColor = aColor;\n"+
            "}\n";

    private final String fragmentShaderCode =
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "varying vec4 vColor;\n" +
            "uniform sampler2D uSampler;\n" +
            "uniform float uT;\n" +
            "void main() {\n" +
            "  vec4 textureColor = texture2D(uSampler, vec2(vTextureCoord.s+uT, vTextureCoord.t));\n" +
            "  gl_FragColor = vColor * textureColor;\n" +
            "}\n";

    static final Neighbor NE = new Neighbor(0, new Vector3D( 0.866f, 0.500f), new Cell( 1, 1));
    static final Neighbor N  = new Neighbor(1, new Vector3D( 0.000f, 1.000f), new Cell( 2, 0));
    static final Neighbor NW = new Neighbor(2, new Vector3D(-0.866f, 0.500f), new Cell( 1,-1));
    static final Neighbor SW = new Neighbor(3, new Vector3D(-0.866f,-0.500f), new Cell(-1,-1));
    static final Neighbor S  = new Neighbor(4, new Vector3D( 0.000f,-1.000f), new Cell(-2, 0));
    static final Neighbor SE = new Neighbor(5, new Vector3D( 0.866f,-0.500f), new Cell(-1, 1));
    static final Neighbor NEIGHBORS[] = {NE, N, NW, SW, S, SE};

    private float mB; // see docs, length of hex size
    private float mR;
    private float mBRes;
    private float mBoardSize;
    private FloatBuffer mUVBuffer; // static
    private FloatBuffer mVertexBuffer; // static, kept for lookup, may become dynamic
    private FloatBuffer mColorBuffer;
    protected int mVertexId;
    protected int mColorId;
    protected int mUVId;
    protected boolean mVertexDynamic = false;
    protected boolean mColorDynamic = false;
    protected boolean mUVDynamic = false;
    private int[][] mBridgeGeometry; // map between connections between hex and associated geometry

    private float mMargin;
    private float mHexWidth;
    private int mNumHex;
    private int mNumRows;
    private int mNumCols;
    private int mNumVertices;

    private float[] mShape;

    HexBoard (float _hexSize, float _boardSize, float _margin)
    {
        mTranslation = new Vector3D(0,0,-8);
        mRotationZ = 0;
        mUniformScale = 1;
        Matrix.setIdentityM(mvMatrix, 0);
        mvTranslate(mTranslation);
        for (int i = 0; i < 4; i++)
        {
            String a1 = Float.toString(mvMatrix[i*4+0]);
            String a2 = Float.toString(mvMatrix[i*4+1]);
            String a3 = Float.toString(mvMatrix[i*4+2]);
            String a4 = Float.toString(mvMatrix[i*4+3]);
            Log.d("GLITZ", a1+" "+ a2+ " "+ a3 +" "+ a4);
        }

        mB = _hexSize;
        mR = mB / (2.0f * (float) Math.tan(Math.toRadians(30))); // see docs
        mBRes = (float) Math.sqrt(mB * mB - mR * mR); // see docs, extends b to edge of bounding square
        mBoardSize = _boardSize;

        mMargin = 1.0f -_margin;
        mNumHex = 0;
        mNumRows = 2 * ( (int) Math.floor(mBoardSize/mR));
        mNumCols = (int) Math.floor((2 * (mBoardSize - mB)/(mB * 3)) + 0.5f);
        mHexWidth = 3 * mB * mNumCols;

        mShape = new float[6*3*3]; // numTriangles * numVerticesPerTri * numCoordsPerVertex
        float angle = (float) Math.toRadians(60.0);
        for (int i = 0; i < 6; i ++)
        {
            mShape[i*9+0] = 0.0f;
            mShape[i*9+1] = 0.0f;
            mShape[i*9+2] = 0.0f;

            mShape[i*9+3] = mB * (float) Math.cos(angle * i);
            mShape[i*9+4] = mB * (float) Math.sin(angle * i);
            mShape[i*9+5] = 0.0f;

            mShape[i*9+6] = mB * (float) Math.cos(angle * (i+1));
            mShape[i*9+7] = mB * (float) Math.sin(angle * (i+1));
            mShape[i*9+8] = 0.0f;
        }

        // init shader
        int vertexShader = LoadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = LoadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        Log.d("GLITZ", vertexShaderCode);
        Log.d("GLITZ", fragmentShaderCode);

        mShader = GLES20.glCreateProgram();
        GLES20.glAttachShader(mShader, vertexShader);
        GLES20.glAttachShader(mShader, fragmentShader);

        GLES20.glLinkProgram(mShader);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(mShader, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE)
        {
            Log.e("GLITZ", "Could not link compile program: ");
            Log.e("GLITZ", GLES20.glGetProgramInfoLog(mShader));
        }

        mPositionHandle = GLES20.glGetAttribLocation(mShader, "aPosition");
        mColorHandle = GLES20.glGetAttribLocation(mShader, "aColor");
        mUVHandle = GLES20.glGetAttribLocation(mShader, "aTextureCoord");

        mTimeHandle = GLES20.glGetUniformLocation(mShader, "uT");
        mSamplerHandle = GLES20.glGetUniformLocation(mShader, "uSampler");
        mMVMatHandle = GLES20.glGetUniformLocation(mShader, "uMVMatrix");
        mPMatHandle = GLES20.glGetUniformLocation(mShader, "uPMatrix");
    }

    int initBuffer(ArrayList<Float> data, FloatBuffer buffer, int type) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.size() * 4);
        bb.order(ByteOrder.nativeOrder());
        for (int i = 0; i < data.size(); i++) {
            Float f = data.get(i);
            if (f == null)
            {
                Log.d("GLITZ", "Why is a float null?!?");
                continue;
            }
            buffer.put(i, f);
        }
        buffer.position(0);
        Log.d("GLITZ", "Buffer capacity "+buffer.capacity()+" Data length "+data.size());

        int[] ids = new int[1];
        GLES20.glGenBuffers(1, ids, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ids[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buffer.capacity() * 4, buffer, type);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        return ids[0];
    }

    void addVertex(float px, float py, float pz,
                   ArrayList<Float> vertexList,
                   ArrayList<Float> textureList,
                   ArrayList<Float> colorList)
    {
        vertexList.add(px);
        vertexList.add(py);
        vertexList.add(pz);

        textureList.add((px+mBoardSize)/(2*mBoardSize));
        textureList.add((py+mBoardSize)/(2*mBoardSize));

        colorList.add(1.0f);
        colorList.add(0.0f);
        colorList.add(1.0f);
        colorList.add(1.0f);
    }

    void initBoard()
    {
        ArrayList<Float> vertexList = new ArrayList<Float>();
        ArrayList<Float> textureList = new ArrayList<Float>();
        ArrayList<Float> colorList = new ArrayList<Float>();

        float startx = -mBoardSize;
        float y = -mBoardSize + mR;
        for (int i = 0; i < mNumRows; i++)
        {
            float x = startx + 2*mB + mBRes;
            if (i % 2 == 1)
            {
                x = startx + mB;
            }

            for (int j = 0; j < mNumCols; j++)
            {
                for (int p = 0; p < mShape.length; p+=3)
                {
                    addVertex(mShape[p] * mMargin + x, mShape[p+1] * mMargin + y, 0,
                         vertexList, textureList, colorList);
                }

                mNumHex++;
                x += 3 * mB;
            }
            y += mR;
        }

        /*
        mBridgeGeometry = new int[mNumHex][6]; // map between idx and starting geometry ID of bridges in each direction
        for (int idx = 0; idx < mNumHex; idx++)
        {
            mBridgeGeometry[idx] = new int[6];
            for (int n = 0; n < 6; n++){ mBridgeGeometry[idx][n] = -1; }

            ArrayList<Vector3D> sides = getHexSidesById(idx, vertexList);
            for (int n = 0; n < NEIGHBORS.length; n++)
            {
                int neighborIdx = getNeighborId(idx, NEIGHBORS[n]);
                if (neighborIdx < idx) // we already created the geometry for it, or -1
                {
                    if (neighborIdx != -1)
                    {
                        int neighborSideId = (n+3) % 6; // get corresponding side from neighbor
                        int bridgeId = mBridgeGeometry[neighborIdx][neighborSideId];
                        mBridgeGeometry[idx][n] = bridgeId;
                    }
                    continue;
                }

                // create bridge
                mBridgeGeometry[idx][n] = vertexList.size() / 3;  // store triangle index so we can show it later

                ArrayList<Vector3D> neighborSides = getHexSidesById(neighborIdx, vertexList);
                int neighborSideId = (n+3) % 6; // get corresponding side from neighbor
                Vector3D neighborSideP1 = neighborSides.get(neighborSideId*2+0);
                Vector3D neighborSideP2 = neighborSides.get(neighborSideId*2+1);

                Vector3D p1 = sides.get(n*2+0);
                Vector3D p2 = sides.get(n*2+1);
                //console.log(neighborIdx, neighborSides[0], neighborSideP1.x, neighborSideP2.x, p1.x, p2.x);

                addVertex(p2.x, p2.y, 0, vertexList, textureList, colorList);
                addVertex(p1.x, p1.y, 0, vertexList, textureList, colorList);
                addVertex(neighborSideP2.x, neighborSideP2.y, 0, vertexList, textureList, colorList);

                addVertex(neighborSideP2.x, neighborSideP2.y, 0, vertexList, textureList, colorList);
                addVertex(neighborSideP1.x, neighborSideP1.y, 0, vertexList, textureList, colorList);
                addVertex(p2.x, p2.y, 0, vertexList, textureList, colorList);
            }
        }
        */

        mVertexBuffer = FloatBuffer.allocate(vertexList.size());
        mColorBuffer = FloatBuffer.allocate(colorList.size());
        mUVBuffer = FloatBuffer.allocate(textureList.size());

        mVertexId = initBuffer(vertexList, mVertexBuffer, GLES20.GL_STATIC_DRAW);
        mColorId = initBuffer(colorList, mColorBuffer, GLES20.GL_DYNAMIC_DRAW);
        mUVId = initBuffer(textureList, mUVBuffer, GLES20.GL_STATIC_DRAW);
        mNumVertices = vertexList.size() / 3;
        mColorDynamic = true;
        mPrimitive = GLES20.GL_TRIANGLES;
        Log.d("GLITZ", "Init board: " + mNumRows + " " + mNumCols + " " + mNumHex + " "+ mNumVertices +
                " colors: "+colorList.size()+" uv: "+ textureList.size()+" vertex: "+vertexList.size());
    }

    static int LoadShader(int type, String shaderSource)
    {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderSource);
        GLES20.glCompileShader(shader);
        int compiled[] = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("GLITZ", "Could not compile shader " + type + ":");
            Log.e("GLITZ", GLES20.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    static void mvPushMatrix() // add to baseclass
    {
        mvMatrixStack.add(mvMatrix);
    }

    static void mvPopMatrix() // add to baseclass
    {
        int lastIdx = mvMatrixStack.size()-1;
        mvMatrix = mvMatrixStack.get(lastIdx);
        mvMatrixStack.remove(lastIdx);
    }

    static void mvTranslate(Vector3D v)
    {
        Matrix.translateM(mvMatrix, 0, v.x, v.y, v.z);
    }

    static void mvRotate(float z)
    {
        Matrix.setRotateEulerM(mvMatrix, 0, 0, 0, z);
    }

    static void mvScale(float s)
    {
        Matrix.scaleM(mvMatrix, 0, s, s, s);
    }

    void _applyTransforms()
    {
        mvTranslate(mTranslation);
        mvRotate(mRotationZ);
        mvScale(mUniformScale);
    }

    void draw(float time, float[] pmatrix, int textureId) {
        if (!mEnabled) return;
        //mvPushMatrix();

        //_applyTransforms();

        GLES20.glUseProgram(mShader);

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexId);
        if (mVertexDynamic) GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVertexBuffer.capacity() * 4, mVertexBuffer, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glEnableVertexAttribArray(mUVHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mUVId);
        if (mUVDynamic) GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mUVBuffer.capacity() * 4, mUVBuffer, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glVertexAttribPointer(mUVHandle, 2, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glEnableVertexAttribArray(mColorHandle);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mColorId);
        if (mColorDynamic) GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mColorBuffer.capacity() * 4, mColorBuffer, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(mSamplerHandle, 0);

        GLES20.glUniformMatrix4fv(mPMatHandle, 1, false, pmatrix, 0);
        GLES20.glUniformMatrix4fv(mMVMatHandle, 1, false, mvMatrix, 0);
        GLES20.glUniform1f(mTimeHandle, time);

        GLES20.glDrawArrays(mPrimitive, 0, mNumVertices);
        //mvPopMatrix();
    }

    ArrayList<Vector3D> getHexSidesById(int idx, ArrayList<Float> vertexList)
    {
        ArrayList<Vector3D> sides = new ArrayList<Vector3D>();
        int offset = idx * mShape.length;
        for (int i = 0; i < 6; i++)
        {
            int tri = i*3*3; // 3 vertices per tri, 3 components oer vertex
            int p2 = 1*3; // want 2nd side, so points 2 and 3
            int p3 = 2*3;

            float x = vertexList.get(offset+tri+p2+0);
            float y = vertexList.get(offset+tri+p2+1);
            sides.add(new Vector3D(x,y));

            x = vertexList.get(offset+tri+p3+0);
            y = vertexList.get(offset+tri+p3+1);
            sides.add(new Vector3D(x,y));
        }
        return sides;
    }

    int getNeighborId(int idx, Neighbor side)
    {
        Cell cell = idToCell(idx);
        int i = cell.i + side.offset.i;
        int j = cell.j + side.offset.j;
        if (isValidHex(new Cell(i,j)))
        {
            return cellToId(new Cell(i,j));
        }
        return -1;
    }

    Cell idToCell(int idx)
    {
        int row = (int) Math.floor(idx/mNumCols);
        int tmp = idx - row * mNumCols;

        int i = 0;
        int j = 0;
        if (row % 2 == 0)
        {
            i = row;
            j = tmp*2+1;
        }
        else
        {
            i = row;
            j = tmp*2;
        }

        return new Cell(i,j);
    }

    int cellToId(Cell cell)
    {
        int idx = 0;
        if (cell.i % 2 == 0)
        {
            idx = cell.i * mNumCols + (cell.j-1)/2;
        }
        else
        {
            idx = cell.i * mNumCols + cell.j/2;
        }
        return idx;
    }

    boolean isValidHex(Cell cell)
    {
        if (cell.i < 0) return false;
        if (cell.j < 0) return false;

        if (cell.i >= mNumRows) return false;
        if (cell.j >= mNumCols*2) return false;

        if (cell.i % 2 == 0 && cell.j % 2 == 0) return false;
        if (cell.i % 2 == 1 && cell.j % 2 == 1) return false;

        return true;
    }



}
