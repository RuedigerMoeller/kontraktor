package sample.httpjs;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;

/**
 * Created by ruedi on 06/06/15.
 */
public class Pojo implements Serializable {

    String name;
    HashSet<Pojo> otherPojos = new HashSet<>();

    public Pojo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addPojo( Pojo other ) {
        otherPojos.add(other);
    }

    @Override
    public String toString() {
        return "Pojo{" +
               "name='" + name + '\'' +
               ", otherPojos=" + otherPojos +
               '}';
    }
}
