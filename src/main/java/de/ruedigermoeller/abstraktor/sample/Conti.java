package de.ruedigermoeller.abstraktor.sample;

/**
 * Created by ruedi on 10.01.14.
 */
public class Conti implements Runnable {


    public void runTest() {
        for (int i=0; i < 50; i++) {

        }
    }

    @Override
    public void run() {
        int count = 0;
        while ( true ) {
            runTest();
            if (count> 10*1000*1000) {
                return;
            }
        }
    }

    public static void main(String[]a) {
    }

}
