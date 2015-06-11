package org.nustaq.kontraktor.remoting.http.javascript;

import org.nustaq.kontraktor.util.Log;
import org.nustaq.kson.Kson;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by ruedi on 06.04.2015.
 *
 * Algorithm: starting from the root component (searched via resource path), resolve all dependencies
 * resulting in a list of (ordered according to dependencies) directories.
 * The resulting list is then searched in order when doing lookup/merge
 */
public class DependencyResolver {

    protected String mergedPrefix;
    protected String lookupPrefix;
    protected File resourcePath[];
    protected File lookupDirs[];
    protected String baseDir = ".";
    protected String rootComponent; // e.g. app (~subapplication)

    public DependencyResolver(String lookupPrefix, String mergedPrefix, String rootComponent, String[] resourcePath) {
        this.lookupPrefix = lookupPrefix;
        this.mergedPrefix = mergedPrefix;
        setResourcePath(resourcePath);
        setRootComponent(rootComponent);
    }

    public String getBaseDir() {
        return baseDir;
    }

    // must be called before setting path and base comp
    protected DependencyResolver setBaseDir(String baseDir) {
        this.baseDir = baseDir;
        return this;
    }

    protected DependencyResolver setResourcePath( String ... path ) {
        resourcePath = new File[path.length];
        for (int i = 0; i < path.length; i++) {
            String dir = baseDir+"/"+path[i];
            File f = new File( dir ).getAbsoluteFile();
            if ( f.exists() && ! f.isDirectory() ) {
                throw new RuntimeException("only directorys can reside on resourcepath");
            }
            resourcePath[i] = f;
        }
        return this;
    }

    // call after setResourcePath
    protected void setRootComponent( String name ) {
        this.rootComponent = name;
        List<File> dependentDirs = null;
        try {
            dependentDirs = getDependentDirs(name, new ArrayList<>(), new HashSet<>());
        } catch (IOException e) {
            e.printStackTrace();
        }
        lookupDirs = new File[dependentDirs.size()];
//        Collections.reverse(dependentDirs); wrong !
        dependentDirs.toArray(lookupDirs);
    }

    public String getRootComponent() {
        return rootComponent;
    }

    /**
     * iterates all component directorys, list each (nonrecursively) and return all component/filenames
     * where filter returns true.
     *
     * @param filter
     * @return
     */
    public List<String> findFilesInDirs( BiFunction<String,String,Boolean> filter ) {
        HashSet<String> done = new HashSet<>();
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < lookupDirs.length; i++) {
            File lookupDir = lookupDirs[i];
            String[] list = lookupDir.list();
            for (int j = 0; j < list.length; j++) {
                String file = list[j];
                if ( !done.contains(file) && filter.apply(lookupDir.getName(),file) ) {
                    result.add(lookupDir.getName()+"/"+file);
                }
                done.add(file);
            }
        }
        return result;
    }

    /**
     * iterate component directories in order and return full path of first file matching
     *
     * @param name
     * @return
     */
    public File locateResource( String name ) {
        // search for explicit includes along resource path (e.g. lookup/dir/lib.js) to allow for exclusion
        // of libs from sripts.js
        for (int i = 0; i < resourcePath.length; i++) {
            File fi = new File(resourcePath[i].getAbsolutePath()+File.separator+name);
            if ( fi.exists() )
                return fi;
        }
        // last resort look flat for a file named "name"
        for (int i = 0; i < lookupDirs.length; i++) {
            File fi = new File(lookupDirs[i],name);
            if ( fi.exists() )
                return fi;
        }
        return null;
    }

    /**
     * @param name
     * @return a priority ordered list of directories containing files for a given component.
     */
    public List<File> locateComponent( String name ) {
        List<File> result = new ArrayList<>();
        for (int i = 0; i < resourcePath.length; i++) {
            File file = resourcePath[i];
            File newOne = new File(file,name);
            if ( newOne.exists() && newOne.isDirectory() ) {
                result.add(newOne);
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
    protected List<File> getDependentDirs(String comp, List<File> li, HashSet<String> alreadyCheckedDependencies) throws IOException {
        if (alreadyCheckedDependencies.contains(comp)) {
            return li;
        }
        alreadyCheckedDependencies.add(comp);
        for (int i = 0; i < resourcePath.length; i++) {
            File file = resourcePath[i];
            File newOne = new File(file,comp).getCanonicalFile();
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
                    li.add(newOne.getCanonicalFile());
            }
        }
        return li;
    }

    /**
     * returns document.write() script tags for each dependent js library.
     * DEV MODE only. In production mode, all .js will be merged into a single document, which is
     * hard to debug with. Therefore in development just generate a bunch of single JS-file includes
     *
     * @param jsFileNames
     * @return
     */
    public byte[] createScriptTags( List<String> jsFileNames ) {
        // inefficient, however SPA's load once, so expect not too many requests
        ByteArrayOutputStream bout = new ByteArrayOutputStream(200000);
        PrintStream ps = new PrintStream(bout);
        for (int i = 0; i < jsFileNames.size(); i++) {
            String f = jsFileNames.get(i);
            if ( f.endsWith(".js") ) {
                Log.Info( this,"   " + f + " size:" + f.length());
                ps.println("document.write(\"<script src='" + lookupPrefix + f+"'></script>\")");
                Log.Info(this, "document.write(\"<script src='" + lookupPrefix + f + "'></script>\")");
            }
        }
        ps.flush();
        byte[] bytes = bout.toByteArray();
        return bytes;
    }

    /**
     * lookup (ordered directory computed by resourcepath) and merge scripts into a single byte[].
     *
     * use in production mode (debugging will be hard)
     *
     * for dev use createScriptTags() instead
     *
     * @param fileNames list script (.js) filenames
     * @return
     */
    public byte[] mergeScripts(List<String> fileNames) {
        // inefficient, however SPA's load once, so expect not too many requests
        ByteArrayOutputStream bout = new ByteArrayOutputStream(200000);
        for (int i = 0; i < fileNames.size(); i++) {
            String jsFileName = fileNames.get(i);
            File f = locateResource(jsFileName);
            String absolutePath = f.getAbsolutePath();
            if ( f.getName().endsWith(".js") ) {
                Log.Info(this, "   " + f.getName() + " size:" + f.length());
                byte[] bytes = new byte[(int) f.length()];
                try (FileInputStream fileInputStream = new FileInputStream(f)) {
                    fileInputStream.read(bytes);
                    bout.write(bytes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        byte[] bytes = bout.toByteArray();
        return bytes;
    }

    /**
     * lookup (ordered directory computed by resourcepath) and merge scripts into a single byte[].
     *
     * @param fileNames list of filenames to merge
     * @return
     */
    public byte[] mergeBinary(List<String> fileNames) {
        // inefficient, however SPA's load once, so expect not too many requests
        ByteArrayOutputStream bout = new ByteArrayOutputStream(200000);
        for (int i = 0; i < fileNames.size(); i++) {
            String jsFileName = fileNames.get(i);
            File f = locateResource(jsFileName);
            String absolutePath = f.getAbsolutePath();
            if ( f != null ) {
                Log.Info(this,"   " + f.getName() + " size:" + f.length());
                byte[] bytes = new byte[(int) f.length()];
                try (FileInputStream fileInputStream = new FileInputStream(f)) {
                    fileInputStream.read(bytes);
                    bout.write(bytes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        byte[] bytes = bout.toByteArray();
        return bytes;
    }

    /**
     * locate ressource file on the directory path and merge them in a document.write file.
     * Does some text replacements (?? forgot why)
     *
     * @param names
     * @return
     */
    public byte[] mergeTextSnippets( List<String> names, String startTag, String endTag ) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(2000);
        HashSet hs = new HashSet();
        PrintStream pout = new PrintStream(bout);
        pout.println("document.write('\\"+startTag);
        for (int i = 0; i < names.size(); i++) {
            String finam = names.get(i);
            File f = locateResource(finam);
            if ( f != null )
            {
                try (FileReader fileInputStream = new FileReader(f)) {
                    BufferedReader in = new BufferedReader(fileInputStream);
                    while (in.ready()) {
                        String line = in.readLine();
                        line = line.replace("\'", "\\'");
                        pout.println(line+"\\");
                    }
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        pout.println(endTag+"');");
        pout.flush();
        return bout.toByteArray();
    }

}
