package org.nustaq.kontraktor.util;

import junit.framework.TestCase;

import org.nustaq.kontraktor.Actors;

public class TicketMachineInitTest extends TestCase {

    public void testGetTicketFailsIfTooFastInit() {
        final TicketMachine tm = new TicketMachine();
        final String channelKey = "test";
        final int counter = 10;

        final PromiseLatch promiseLatch = new PromiseLatch(counter);

        for (int i = 0; i < counter; i++) {
            final int ii = i;

            Actors.exec.execute(() -> {
                System.out.println("setting up ticket " + ii);
                // fails without incrementing sleep or without `TicketMachine > synchronized getTicket(final Object channelKey)`
                //sleep(ii*100);

                tm.getTicket(channelKey).onResult((finSig) -> {
                    System.out.println("finish " + ii);
                    promiseLatch.countDown();
                    finSig.resolve();
                });
            });
        }

        promiseLatch.getPromise().then(() -> {
            System.out.println("all done");
        }).await();
    }

    private static void sleep(final int sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}