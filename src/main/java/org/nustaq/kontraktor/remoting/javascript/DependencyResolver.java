package org.nustaq.kontraktor.remoting.javascript;

import org.nustaq.kson.Kson;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

/**
 * Created by ruedi on 06.04.2015.
 */
public class DependencyResolver {

    public static boolean DevMode = true;
    File resourcePath[];
    File lookupDirs[];

    public DependencyResolver setResourcePath( String ... path ) {
        resourcePath = new File[path.length];
        for (int i = 0; i < path.length; i++) {
            String dir = path[i];
            File f = new File( dir );
            if ( f.exists() && ! f.isDirectory() ) {
                throw new RuntimeException("only directorys can reside on resourcepath");
            }
            resourcePath[i] = f;
        }
        return this;
    }

    public void setRootComponent( String name ) {
        List<File> dependentDirs = getDependentDirs(name, new ArrayList<>(), new HashSet<>());
        lookupDirs = new File[dependentDirs.size()];
        dependentDirs.toArray(lookupDirs);
    }

    public List<String> findFilesInDirs( Function<String,Boolean> filter ) {
        HashSet<String> done = new HashSet<>();
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < lookupDirs.length; i++) {
            File lookupDir = lookupDirs[i];
            String[] list = lookupDir.list();
            for (int j = 0; j < list.length; j++) {
                String file = list[j];
                if ( !done.contains(file) && filter.apply(file) ) {
                    result.add(file);
                }
                done.add(file);
            }
        }
        return result;
    }

    /**
     * creates a lookup path for given component.
     * (1) resourcePath is searched for a directory named 'comp', file is added
     * (2) if the newly added directory contains a 'dep.kson' dependency file, recursively coninue wiht (1) for each dependent component
     *
     * @param comp - component name to lookup
     * @param li - growing list of files
     * @param alreadyCheckedDependencies - already visited components
     * @return an ordered list of directories which can be searched later on when doing single file lookup
     */
    protected List<File> getDependentDirs(String comp, List<File> li, HashSet<String> alreadyCheckedDependencies) {
        if (alreadyCheckedDependencies.contains(comp)) {
            return li;
        }
        alreadyCheckedDependencies.add(comp);
        for (int i = 0; i < resourcePath.length; i++) {
            File file = resourcePath[i];
            File newOne = new File(file,comp);
            if ( li.contains(newOne) )
                continue;
            if ( newOne.exists() && newOne.isDirectory() ) {
                File dep = new File(newOne, "dep.kson");
                if ( dep.exists() ) {
                    try {
                        String deps[] = ((ModuleProperties) new Kson().map(ModuleProperties.class).readObject(dep, ModuleProperties.class)).depends;
                        for (int j = 0; j < deps.length; j++) {
                            getDependentDirs(deps[j],li,alreadyCheckedDependencies);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if ( ! li.contains(newOne) )
                    li.add(newOne);
            }
        }
        return li;
    }

    public static void main(String a[]) {
        DependencyResolver dep = new DependencyResolver();
        dep.setResourcePath(
            "4k",
            "tmp",
            "../../weblib",
            "../../weblib/nustaq",
            "../../weblib/knockout"
        );
        dep.setRootComponent("app");
        System.out.println(Arrays.toString(dep.lookupDirs));

        dep.findFilesInDirs( fnme -> fnme.endsWith(".js") ).forEach( res -> System.out.println(res) );
        dep.findFilesInDirs( fnme -> fnme.endsWith(".tpl.html") ).forEach( res -> System.out.println(res) );
    }
}
