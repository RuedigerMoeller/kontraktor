package org.nustaq.kontraktor.kollektiv;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by moelrue on 3/6/15.
 */
public class KollektivMember extends Actor<KollektivMember> {

    String tmpDir = "/tmp";
    List<String> masterAddresses;
    HashMap<String,KollektivMaster> masters;
    String nodeId = "SL"+System.currentTimeMillis()+":"+(System.nanoTime()&0xffff);
    String clazzDirBase = "/tmp";
    HashMap<String, ActorAppBundle> apps = new HashMap<>();


    public void $init() {
        List<String> addr = new ArrayList<>();
        addr.add("127.0.0.1:3456");
        $initWithOptions(addr);
    }

    public void $initWithOptions(List<String> masterAddresses) {
        this.masterAddresses = masterAddresses;
        masters = new HashMap<>();
        $startHB();
    }

    public void $startHB() {
        checkThread();
        for (int i = 0; i < masterAddresses.size(); i++) {
            String s = masterAddresses.get(i);
            if ( masters.get(s) == null ) {
                String[] split = s.split(":");
                try {
                    TCPActorClient.Connect(
                        KollektivMaster.class,
                        split[0], Integer.parseInt(split[1]),
                        disconnectedRef -> self().$refDisconnected(s,disconnectedRef)
                    )
                    .onResult(actor -> {
                        masters.put(s, actor);
                        actor.$registerMember(new MemberDescription(nodeId), self());
                    })
                    .onError(err -> System.out.println("failed to connect " + s));
                } catch (IOException e) {
                    System.out.println("could not connect "+e);
                }
            }
        }
        delayed(3000, () -> self().$startHB());
    }

    public void $refDisconnected(String address, Actor disconnectedRef) {
        checkThread();
        System.out.println("actor disconnected "+disconnectedRef+" address:"+address);
        masters.remove(address);
    }

    public static class ActorBootstrap {

        public Actor actor;

        public ActorBootstrap(Class actor) {
            this.actor = Actors.AsActor(actor);
        }

    }

    public Future<Actor> $run(String clazzname, String nameSpace) {
        Promise res = new Promise();
        try {
            ActorAppBundle actorAppBundle = apps.get(nameSpace);
            MemberClassLoader loader = actorAppBundle.getLoader();
            Class<?> actorClazz = loader.loadClass(clazzname);
            Class<?> bootstrap = loader.loadClass(ActorBootstrap.class.getName());
            Object actorBS = bootstrap.getConstructor(Class.class).newInstance(actorClazz);
            Field f = actorBS.getClass().getField("actor");
            Actor resAct = (Actor) f.get(actorBS);
            res.receive(resAct,null);
        } catch (Exception e) {
            e.printStackTrace();
            res.receive(null,e);
        }
        return res;
    }

    public Future $defineNameSpace( ActorAppBundle bundle ) {
        try {
            ActorAppBundle actorAppBundle = apps.get(bundle.getName());
            if ( actorAppBundle != null ) {
                actorAppBundle.getMainActor().$stop();
            }
            System.out.println("define name space "+bundle.getName()+" size "+bundle.getSizeKB() );
            File base = new File(tmpDir + File.separator + bundle.getName());
            base.mkdirs();
            bundle.getResources().entrySet().forEach(entry -> {
                if (entry.getKey().endsWith(".jar")) {
                    String name = new File(entry.getKey()).getName();
                    try {
                        Files.write(Paths.get(base.getAbsolutePath(), name), entry.getValue().bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        new File(base.getAbsolutePath() + File.separator + entry.getKey()).getParentFile().mkdirs();
                        Files.write(Paths.get(base.getAbsolutePath(), entry.getKey()), entry.getValue().bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            bundle.setBaseDir(base.getAbsolutePath());
            MemberClassLoader memberClassLoader = new MemberClassLoader(bundle, new URL[]{base.toURL()}, getClass().getClassLoader());
            bundle.setLoader(memberClassLoader);
            apps.put(bundle.getName(),bundle);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return new Promise<>(null);
    }

    public static void main( String a[] ) {
        KollektivMember sl = Actors.AsActor(KollektivMember.class);
        sl.$init();
        sl.$refDisconnected(null,null);
    }

}
