kontraktor
==========

lightweight and efficient Actor implementation in Java

Kontraktor implements a typed actor model to avoid message definition and handlng boilerplate code. Additionally this integrates well
with code comletion of IDEs.

Kontraktor creates a runtime-generated proxy instance which puts all calls onto a queue. The real actor instance then reads 
method invocations (=messages) from this queue in a dedicated thread.

E.g.

```java
    public static class BenchSub extends Actor {
        int count;
        
        public void benchCall(String a, String b, String c) {
            count++;
        }
          
        public void getResult( Callback<Integer> cb ) {
            cb.receiveResult(count,null);
        }
    }

    public static main(..) 
    {
        final BenchSub bsProxy = Actors.SpawnActor(BenchSub.class); // create proxy + actor instance
        for (int i : new int[10] ) {
            bsProxy.benchCall("u", "o", null); // actually enqueues
        }
        // all communication is async
        bsProxy.getResult( new Callback<Integer>() {
            @Override
            public void receiveResult(Integer result, Object error) {
                bs.stop();
            }
        });
    }
´´´

Kontrakor internally uses a high performance queue implementation of the Jaq-In-A-Box project and can pass 
9 million messages per second (on i7 laptop, method with 3 arguments passed).

[documentation+maven still undone]
