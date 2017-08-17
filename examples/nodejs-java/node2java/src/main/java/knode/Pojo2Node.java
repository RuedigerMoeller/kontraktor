package knode;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by ruedi on 02/08/15.
 */
public class Pojo2Node implements Serializable {

    String name;
    int someInt;
    double someDouble;


    String names[];
    int someInts[];
    double someDoubles[];

    String namesEmpty[];
    int someIntsEmpty[];
    double someDoublesEmpty[];

    HashMap embeddedMap;

    public Pojo2Node initVals() {

        name ="Name"; someInt=-345345; someDouble = 123.45678;

        names = new String[] { "Ä", "Ü", "Ö" };
        someInts = new int[] { 13,14,-15};
        someDoubles = new double[] { 133.0,143.033,-15.33311};

        namesEmpty = new String[0];
        someIntsEmpty = new int[0];
        someDoublesEmpty = new double[0];

        embeddedMap = new HashMap();
        embeddedMap.put("x","y");
        embeddedMap.put(1313, new Object[] {1,3,45} );
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSomeInt() {
        return someInt;
    }

    public void setSomeInt(int someInt) {
        this.someInt = someInt;
    }

    public double getSomeDouble() {
        return someDouble;
    }

    public void setSomeDouble(double someDouble) {
        this.someDouble = someDouble;
    }

    public String[] getNames() {
        return names;
    }

    public void setNames(String[] names) {
        this.names = names;
    }

    public int[] getSomeInts() {
        return someInts;
    }

    public void setSomeInts(int[] someInts) {
        this.someInts = someInts;
    }

    public double[] getSomeDoubles() {
        return someDoubles;
    }

    public void setSomeDoubles(double[] someDoubles) {
        this.someDoubles = someDoubles;
    }

    public String[] getNamesEmpty() {
        return namesEmpty;
    }

    public void setNamesEmpty(String[] namesEmpty) {
        this.namesEmpty = namesEmpty;
    }

    public int[] getSomeIntsEmpty() {
        return someIntsEmpty;
    }

    public void setSomeIntsEmpty(int[] someIntsEmpty) {
        this.someIntsEmpty = someIntsEmpty;
    }

    public double[] getSomeDoublesEmpty() {
        return someDoublesEmpty;
    }

    public void setSomeDoublesEmpty(double[] someDoublesEmpty) {
        this.someDoublesEmpty = someDoublesEmpty;
    }

    public HashMap getEmbeddedMap() {
        return embeddedMap;
    }

    public void setEmbeddedMap(HashMap embeddedMap) {
        this.embeddedMap = embeddedMap;
    }
}
