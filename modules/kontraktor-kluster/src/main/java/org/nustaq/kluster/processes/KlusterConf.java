package org.nustaq.kluster.processes;

import com.beust.jcommander.JCommander;
import org.nustaq.kontraktor.util.Pair;
import org.nustaq.kson.Kson;

import java.io.*;
import java.util.*;
import java.util.function.Function;

/**
 * Created by ruedi on 18/04/16.
 */
public class KlusterConf extends HashMap implements Serializable {

    List<StarterClientArgs> toStart = new ArrayList<>();
    Set<String> groups;
    public KlusterConf(HashSet<String> groups, String file) throws Exception {
        this.groups = groups;
        Object prog[] = (Object[]) new Kson().readObject(new File(file), Object[].class);
        Properties properties = ProcessStarter.locateProps(0, new File("./"), "troll.properties");
        interpret(properties, prog);
    }

    public List<StarterClientArgs> getToStart() {
        return toStart;
    }

    public void interpret(Properties par, Object[] prog) {
        Properties env = new Properties(par);
        int index = 0;
        while( index < prog.length ) {
            Pair def = new Pair( prog[index], prog[index+1]);
            index += 2;
            if ( def.car().toString().startsWith("loop_")) {
                String[] split = def.car().toString().split("_");
                int start = Integer.parseInt(split[1]);
                int end = Integer.parseInt(split[2]);
                for ( int i = start; i < end; i++ ) {
                    List cdr = (List) def.cdr();
                    Object newOb[] = new Object[cdr.size()];
                    cdr.toArray(newOb);
                    Properties props = new Properties(env);
                    props.put("IDX",""+i);
                    interpret(props, newOb);
                }
            } else if ( "process".equals( def.car() ) ) {
                // resolve commandline string
                String resolved[] = dequote(resolve((String) def.cdr(), x -> (String) env.getProperty(x)));
                StarterClientArgs args = new StarterClientArgs();
                JCommander jc = new JCommander(args);
                args = StarterClient.parseStarterConf(resolved,args,jc);
//                System.out.println(args);
                toStart.add(args);
            } else {
                if ( def.cdr() instanceof List ) {
                    if ( groups == null || groups.size() == 0 || groups.contains(def.car()) ) {
                        System.out.println("start group "+def.car());
                        List cdr = (List) def.cdr();
                        Object newOb[] = new Object[cdr.size()];
                        cdr.toArray(newOb);
                        interpret(env, newOb);
                    } else {
                        System.out.println("ignoring group "+def.car());
                    }
                } else {
                    env.put(def.car(),def.cdr());
                }
            }
        }
    }

    private String[] dequote(String in) {
        ArrayList<String> result = new ArrayList<>();
        int state = 0;
        String var = "";
        for (int i=0; i < in.length(); i++ ) {
            char c = in.charAt(i);
            if ( state == 0 ) {
                if ( Character.isWhitespace(c) ) {
                    if (var.length()>0) {
                        result.add(var);
                        var = "";
                    }
                } else {
                    if ( c=='\'' ) {
                        if (var.length()>0) {
                            result.add(var);
                            var = "";
                        }
                        state = 1;
//                        var += c;
                    } else {
                        var += c;
                    }
                }
            } else
            if ( state == 1 ) {
                if ( c=='\'' ) {
                    if (var.length()>0) {
                        result.add(var);
                        var = "";
                    }
                    state = 0;
                } else {
                    var += c;
                }
            }
        }
        String resarr[] = new String[result.size()];
        result.toArray(resarr);
        return resarr;
    }

    private String resolve(String cdr, Function<String,String> map) {
        StringBuilder res = new StringBuilder(cdr.length()*2);
        int state = 0;
        String var = "";
        for (int i=0; i < cdr.length(); i++ ) {
            char c = cdr.charAt(i);
            if ( c == '$' ) {
                state = 1;
            } else {
                switch (state) {
                    case 0:
                        res.append(c); break;
                    case 1: {
                        if ( Character.isLetterOrDigit(c) ) {
                            var += c;
                        } else {
                            String apply = map.apply(var);
                            if ( apply != null )
                                res.append(apply);
                            else
                                res.append("$"+var);
                            state = 0; i--; var = "";
                        }
                        break;
                    }
                }
            }
        }
        String expanded = res.toString();
        if ( ! expanded.equals(cdr) )
            return resolve(expanded,map);
        return expanded;
    }

    public static void main(String[] args) throws Exception {
        HashSet<String> gr = new HashSet<>();
//        gr.add("base");
        KlusterConf conf = new KlusterConf(gr,"/home/ruedi/projects/Juptr/run/prod/cluster.kson");

        conf.getToStart().forEach( s -> System.out.println(s));

        System.out.println(conf);
    }
}
