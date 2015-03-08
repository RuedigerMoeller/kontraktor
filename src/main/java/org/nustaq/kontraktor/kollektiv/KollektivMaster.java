package org.nustaq.kontraktor.kollektiv;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.tcp.TCPActorServer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Created by moelrue on 3/6/15.
 */
public class KollektivMaster extends Actor<KollektivMaster> {

    List<KollektivMember> members;
    ActorAppBundle cachedBundle;

    public void $init() {
        members = new ArrayList<>();
    }

    public void $registerMember(MemberDescription sld, KollektivMember memb) {
        System.out.println("receive registration " + sld + " members:" + members.size() + 1);
        members.add(memb);
        memb.$defineNameSpace(getCachedBundle());
    }

    ActorAppBundle getCachedBundle() {
        if ( cachedBundle == null ) {
            cachedBundle = new ActorAppBundle("Hello");
            buildAppBundle(cachedBundle);
        }
        return cachedBundle;
    }

    void buildAppBundle(ActorAppBundle bundle) {
        String cp = System.getProperty("java.class.path");
        String bcp = System.getProperty("sun.boot.class.path");
        String[] path = cp.split(File.pathSeparator);
        Set<String> bootCP = Arrays.stream(bcp.split(File.pathSeparator)).collect(Collectors.toSet());
        for (int i = 0; i < path.length; i++) {
            String s = path[i];
            if ( ! bootCP.contains(s) &&
                 s.indexOf("jre"+File.separator+"lib"+File.separator) < 0
               )
            {
                File pathOrJar = new File(s);
                if (s.endsWith(".jar") && pathOrJar.exists()) {
                    try {
                        bundle.put(pathOrJar.getName(), Files.readAllBytes(Paths.get(s)) );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (pathOrJar.isDirectory() && pathOrJar.exists()) {
                    try {
                        Files.walk(Paths.get(s), 65536).forEach(p -> {
                            File file = p.toAbsolutePath().toFile();
                            if (!file.isDirectory()) {
                                try {
                                    String rel = p.toString();
                                    rel = rel.substring(s.length(),rel.length());
                                    rel.replace(File.separatorChar,'/');
                                    bundle.put( rel, Files.readAllBytes(p));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("ignore " + s);
                }
            }
        }
    }

    public void $memberDisconnected(Actor closedActor) {
        members.remove(closedActor);
        System.out.println("member disconnected "+closedActor+" members remaining:"+members.size());
    }

    public Future $onMemberMoreThan(int i) {
        Promise p = new Promise();
        AtomicReference<Runnable> toRun = new AtomicReference<>();
        toRun.set( ()-> {
            if (members.size() >= i) {
                p.signal();
            } else {
                delayed(1000,toRun.get());
            }
        });
        delayed(100, toRun.get());
        return p;
    }

    public Future<Actor> $run(Class<TestActor> testActorClass, String nameSpace) {
        if ( members.size() == 0 ) {
            return new Promise<>(null,"no members avaiable");
        }
        Promise res = new Promise<>();
        members.get(0).$run(testActorClass.getName(), nameSpace).then( (r, e) -> {
            res.receive(r, e);
        });
        return res;
    }

    public static void main(String arg[]) throws Exception {
        KollektivMaster cm = Actors.AsActor(KollektivMaster.class);
        cm.$init();
        try {
            TCPActorServer.Publish(cm, 3456, closedActor -> cm.$memberDisconnected(closedActor));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        cm.$onMemberMoreThan(1).onResult(dummy -> {
            cm.$run(TestActor.class, "Hello")
                    .onError(e -> {
                        System.out.println(e);
                    })
                    .onResult(act -> {
                        TestActor tact = (TestActor) act;
                        System.out.println(tact);
                        tact.$init();
                        tact.$method("Huhu Hoho").onResult( res -> System.out.println(res) );
                    });

        });
    }


}
