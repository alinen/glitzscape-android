package org.whatevers.alinen.glitzscape;

/**
 * Created by alinen on 10/20/2016.
 */

public class Neighbor {
    public int value;
    public Vector3D dir;
    public Cell offset;

    public Neighbor(int _v, Vector3D _dir, Cell _offset) {
        value = _v;
        dir = _dir;
        offset = _offset;
    }
};
