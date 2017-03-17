package wapi;

import java.io.Serializable;

/**
 * Created by ruedi on 17.03.17.
 */
public class ForeignClass implements Serializable {

    int x,y,z;

    public ForeignClass(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public String toString() {
        return "ForeignClass{" +
            "x=" + x +
            ", y=" + y +
            ", z=" + z +
            '}';
    }
}
