package de.ruedigermoeller.kontraktor.util;

import de.ruedigermoeller.kontraktor.Callback;
import de.ruedigermoeller.kontraktor.Future;
import de.ruedigermoeller.kontraktor.Promise;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ruedi on 02.06.14.
 *
 * Single threaded, can be used from within one actor only !
 *
 * Problem:
 * you receive a stream of events related to some items (e.g. UserSessions or Trades)
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
 *     endsignalFuture.receiveResult("done",null);
 *   });
 *
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
            futures = new LinkedList<>();
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
            public void receiveResult(Object result, Object error) {
//                System.out.println("rec "+channelKey+" do remove+checknext");
                boolean remove = finalFutures.remove(ticket);
                if ( ! remove )
                    System.err.println("Error failed to remove "+channelKey);
                checkNext(channelKey, finalFutures, ticket);
            }

        });
        if ( futures.size() == 1 ) { // this is the one and only call, start immediately
            signalStart.receiveResult(signalFin,null);
        }
        return signalStart;
    }

    private void checkNext(Object channelKey, List<Ticket> futures, Ticket ticket) {
        if ( futures.size() == 0 ) {
            tickets.remove(channelKey);
//            System.out.println("**remove "+channelKey);
        } else {
//            System.out.println("continue "+channelKey);
            Ticket nextTicket = futures.get(0);
            nextTicket.signalProcessingStart.receiveResult(nextTicket.signalProcessingFinished,null);
        }
    }

    public HashMap<Object, List<Ticket>> getTickets() {
        return tickets;
    }
}
