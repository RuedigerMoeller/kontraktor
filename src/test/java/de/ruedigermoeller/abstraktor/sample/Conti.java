package de.ruedigermoeller.abstraktor.sample;

import de.ruedigermoeller.abstraktor.Actor;
import static de.ruedigermoeller.abstraktor.Actors.*;

import de.ruedigermoeller.abstraktor.ChannelReceiver;
import de.ruedigermoeller.abstraktor.impl.ChannelActor;

/**
 * Created by ruedi on 10.01.14.
 */
public class Conti {

    public static class Ping extends Actor {

        Pong pong;

        public void init() {
            pong = SpawnActor(Pong.class);
        }

        public void sendPings(final int num) {
            ChannelActor<Integer> queue = Channel(new ChannelReceiver<Integer>() {
                int count = 0;
                @Override
                public void receiveResult(Integer val) {
//                    System.out.println("res "+val);
                    if ( count != val.intValue() )
                        System.out.println("ORDER VIOLATION");
                    if (count++ == num - 1) {
                        System.out.println("Done !");
                        System.exit(0);
                    }
                }
            });
            for ( int i = 0; i < num; i++ ) {
                pong.sendPong(i, queue);
            }
        }

    }

    public static class Pong extends Actor {

        public void sendPong( int n, ChannelActor<Integer> rec ) {
            rec.receiveResult(n);
        }

    }

    public static void main(String[]a) {
        Thread.currentThread().setName("Main Thread");
        Ping ping = AsActor(Ping.class);
        ping.init();
        ping.sync();
        ping.sendPings(2000000);
    }

}
