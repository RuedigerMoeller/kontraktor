## Kontraktor-bare-bone client

1.7 / Android compatible client

Supports connecting to remote actors without requiring access to any remote class.
Supports Http Long Poll / json encoding only.

example:

```java
RemoteActorConnection act = new RemoteActorConnection(false);
act.connect("http://localhost:8080/api",false).then(new Callback<RemoteActor>() {
    @Override
    public void receive(RemoteActor result, Object error) {
        System.out.println("result:" + result+" err:"+error );
        result.ask("login", "user", "password").then(new Callback<RemoteActor>() {
            @Override
            public void receive(RemoteActor session, Object error) {
                System.out.println("session Actor received: "+session);
                session.ask("getToDo").then(new Callback<ArrayList>() {
                    @Override
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
