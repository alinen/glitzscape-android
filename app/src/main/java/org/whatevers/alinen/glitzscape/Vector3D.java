package org.whatevers.alinen.glitzscape;

/**
 * Created by alinen on 10/19/2016.
 */

public class Vector3D {

    public float x;
    public float y;
    public float z;

    Vector3D(float _x, float _y, float _z)
    {
        x = _x;
        y = _y;
        z = _z;
    }

    Vector3D(float _x, float _y)
    {
        x = _x;
        y = _y;
        z = 0;
    }

}
