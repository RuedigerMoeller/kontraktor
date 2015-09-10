package play;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by moelrue on 10.09.2015.
 */
public class WN {

    HashMap<String,WNWNoun> id2noun = new HashMap<>();
    HashMap<String,List<WNWNoun>> word2Noun = new HashMap<>();

    static class WNWNoun {
        List<String> nouns = new ArrayList<>();
        String id;
        String superCl;

        public WNWNoun noun(final String noun) {
            nouns.add(noun);
            return this;
        }

        public WNWNoun id(final String id) {
            this.id = id;
            return this;
        }

        public WNWNoun superCl(final String superCl) {
            this.superCl = superCl;
            return this;
        }

        public String getSuperCl() {
            return superCl;
        }

        public String getNoun() {
            return nouns.get(0);
        }

        public List<String> getNouns() {
            return nouns;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return "WNWNoun{" +
                    "nouns=" + nouns +
                    ", id='" + id + '\'' +
                    ", superCl='" + superCl + '\'' +
                    '}';
        }
    }

    public void scan(File f) throws FileNotFoundException {
        Scanner sc = new Scanner(new FileInputStream(f),"UTF-8");
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            String[] split = line.split(" ");
            WNWNoun noun = new WNWNoun().id(split[0]).noun(split[4]);
            addNoundW2N(noun, split[4]);
            for (int i = 7; i < split.length; i+=2) {
                String s = split[i];
                if ( "~".equals(s) ) {
                    break;
                } else
                if ( "@".equals(s) ) {
                    noun.superCl(split[i+1]);
                    break;
                } else {
                    noun.noun(s);
                    addNoundW2N(noun, s);
                }
            }
            id2noun.put(noun.getId(), noun);
        }
    }

    private void addNoundW2N(WNWNoun noun, String s) {
        List<WNWNoun> wnwNouns = word2Noun.get(s);
        if (wnwNouns == null) {
            wnwNouns = new ArrayList<>();
            word2Noun.put(s,wnwNouns);
        }
        wnwNouns.add(noun);
    }

    void dumpChildren(String word) {
        System.out.println("- children of " + word);
        List<WNWNoun> list = word2Noun.get(word);
        for (int i = 0; i < list.size(); i++) {
            WNWNoun wnwNoun = list.get(i);
            dumpChildren(wnwNoun, "  ");
        }
    }

    void dumpChildren(WNWNoun wnwNoun, String bl) {
        id2noun.values().stream()
            .filter( noun -> wnwNoun.getId().equals(noun.getSuperCl()) )
            .forEach( noun -> {
                System.out.println(bl+noun.getNoun());
                if ( bl.length() < 10 ) {
                    dumpChildren(noun,bl+"  ");
                }
            });
    }

    void dumpHierarchy(String word) {
        System.out.println("- " + word);
        List<WNWNoun> list = word2Noun.get(word);
        for (int i = 0; i < list.size(); i++) {
            WNWNoun wnwNoun = list.get(i);
            dumpHierarchy(wnwNoun);
            System.out.println();
        }
    }


    void dumpHierarchy( WNWNoun wnwNoun) {
        if ( wnwNoun == null )
            return;
        WNWNoun superNoun = null;
        if ( wnwNoun.getSuperCl() != null )
            superNoun = id2noun.get(wnwNoun.getSuperCl());
        System.out.print("" + wnwNoun.getNoun() + " ");
        if ( superNoun != null ) {
            dumpHierarchy(superNoun);
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        File f = new File("D:\\work\\dict\\data.noun");
        WN wn = new WN();
        wn.scan(f);

//        wn.dumpHierarchy("dishwashing");
//        wn.dumpHierarchy("flower");
//        wn.dumpHierarchy("plant");
//        wn.dumpHierarchy("inflation_rate");
        wn.dumpChildren("sport");
        wn.dumpChildren("rock_climbing");
//        wn.dumpChildren("");
    }

}
