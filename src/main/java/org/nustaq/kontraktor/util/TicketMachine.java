package org.nustaq.kontraktor.util;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ruedi on 02.06.14.
 *
 * Single threaded, can be used from within one actor only !
 *
 * Problem:
 * you settle a stream of events related to some items (e.g. UserSessions or Trades)
 * you need to do some asynchronous lookups during processing (e.g. query data async)
 * now you want to process parallel, but need to process events related to a single item
 * in order (e.g. want to process trades related to say BMW in the order they come in).
 *
 * usage:
 *
 * ticketMachine.getTicket( "BMW" ).then(
 *   (endSignalfuture,e) -> {
 *
 *     .. wild async processing ..
 *
 *     endsignalFuture.settle("done",null); // will execute next event on bmw if present
 *   });
 *
 * Note Actor.serialOn internally makes use of this
 */
public class TicketMachine {

    static class Ticket {
        Ticket(Future signalProcessingStart, Future signalProcessingFinished) {
            this.signalProcessingStart = signalProcessingStart;
            this.signalProcessingFinished = signalProcessingFinished;
        }
        Future signalProcessingStart;
        Future signalProcessingFinished;
    }

    HashMap<Object,List<Ticket>> tickets = new HashMap<>();
    public Future<Future> getTicket( final Object channelKey ) {
        List<Ticket> futures = tickets.get(channelKey);
        if ( futures == null ) {
            futures = new ArrayList<>(3);
            tickets.put(channelKey,futures);
        }

        Promise<Object> signalFin = new Promise<>();
        Future signalStart = new Promise();
        final Ticket ticket = new Ticket(signalStart,signalFin);
        futures.add(ticket);

//        System.out.println("get ticket "+ticket+" "+Thread.currentThread().getName());

        final List<Ticket> finalFutures = futures;
        signalFin.then(new Callback() {
            @Override
            public void settle(Object result, Object error) {
//                System.out.println("rec "+channelKey+" do remove+checknext");
                boolean remove = finalFutures.remove(ticket);
                if ( ! remove )
                    System.err.println("Error failed to remove "+channelKey);
                checkNext(channelKey, finalFutures, ticket);
            }

        });
        if ( futures.size() == 1 ) { // this is the one and only call, start immediately
            signalStart.settle(signalFin, null);
        }
        return signalStart;
    }

    private void checkNext(Object channelKey, List<Ticket> futures, Ticket ticket) {
        if ( futures.size() == 0 ) {
            tickets.remove(channelKey);
        } else {
            Ticket nextTicket = futures.get(0);
            nextTicket.signalProcessingStart.settle(nextTicket.signalProcessingFinished, null);
        }
    }

    public HashMap<Object, List<Ticket>> getTickets() {
        return tickets;
    }
}
