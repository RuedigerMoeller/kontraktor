## Kontraktor 3 bare bone client

1.7 / Android compatible client

Supports connecting to remote actors without requiring access to any remote class and few dependencies.
Supports Http Long Poll / json encoding only. 
Supports transparent Remote- ActorRefs,Promise,Callback routing but cannot publish an Actor by itself (pure client).

This should be used to connect user client applications (e.g. mobiles), for system interconnect always prefer full kontraktor lib (order of a magnitude faster).

example:
1.7 / Android
```java
RemoteActorConnection act = new RemoteActorConnection(false);
act.connect("http://localhost:8080/api",false).then(new Callback<RemoteActor>() {

    public void receive(RemoteActor facade, Object error) {
        System.out.println("result:" + facade+" err:"+error );

        facade.ask("login", "user", "password").then(new Callback<RemoteActor>() {
            public void receive(RemoteActor session, Object error) {
                System.out.println("session Actor received: "+session);

                session.ask("getToDo").then(new Callback<ArrayList>() {
                    public void receive(ArrayList result, Object error) {
                        for (int i = 0; i < result.size(); i++) {
                            Object o = result.get(i);
                            System.out.println(o);
                        }
                    }
                });
            }
        });
    }
});
```
(or nicer with 1.8):
```java
RemoteActorConnection act = new RemoteActorConnection(false);
act.connect("http://localhost:8080/api",false).then( (facade, error) -> {
    System.out.println("result:" + facade+" err:"+error );
    
    facade.ask("login", "user", "password").then( (session, error) -> {
        System.out.println("session Actor received: "+session);
        
        session.ask("getToDo").then( (result, error) -> {
            for (int i = 0; i < result.size(); i++) {
                Object o = result.get(i);
                System.out.println(o);
            }
        });
    });
});
```
