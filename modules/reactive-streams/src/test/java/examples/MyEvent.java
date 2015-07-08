package examples;

import java.io.Serializable;

/**
 * Created by ruedi on 08/07/15.
 */
public class MyEvent implements Serializable {
    int num;
    double price;
    String name;

    public MyEvent(int num, double price, String name) {
        this.num = num;
        this.price = price;
        this.name = name;
    }
}
