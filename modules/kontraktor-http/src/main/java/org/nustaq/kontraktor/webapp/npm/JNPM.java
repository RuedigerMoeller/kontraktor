package org.nustaq.kontraktor.webapp.npm;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.github.yuchi.semver.Range;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.utils.AsyncHttpActor;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JNPM extends Actor<JNPM> {

    public enum InstallResult {
        EXISTS,
        INSTALLED,
        NOT_FOUND,
        FAILED,
        VERSION_MISMATCH,
    }

    File nodeModulesDir;
    AsyncHttpActor http;


    JNPMConfig config;

    public void init(File nodeModules, JNPMConfig config ) {
        nodeModulesDir = nodeModules;
        http = AsyncHttpActor.getSingleton();
        this.config = config;
    }

    protected String getVersion(String module, String spec, List<String> versions, JsonObject finalDist) {
        if ( spec == null ) {
            return "latest";
        }
        String conf = config.getVersion(module);
        if ( conf != null ) {
            spec = conf;
        }
        String dist = finalDist.getString(spec, null);
        if ( dist != null )
            return dist;
        String bestMatch[] = {"latest"};
        try {
            String finalSpec = spec;
            versions.forEach( vers -> {
                try {
                    if (matches(vers, finalSpec))
                        bestMatch[0] = vers;
                } catch (Exception e) {
                    Log.Warn(this, "cannot parse version tag:'" + vers + "'. Default to latest.");
                    //            if ( versions.getString(spec,"latest") != null ) {
                    //                return versions.getString(spec,"latest");
                    //            }
                }
            });
        } catch (Exception e) {
            Log.Warn(this, "cannot parse version condition:'" + spec + "'. Default to latest.");
        }
        return bestMatch[0];
    }

    private static boolean matches(String version, String condition) {
        Range r = Range.from(condition,false);
        com.github.yuchi.semver.Version v = com.github.yuchi.semver.Version.from(version,false);
        return r.test(v);
    }

    public IPromise<InstallResult> npmInstall( String module, String versionSpec, File importingModuleDir ) {
        if (versionSpec == null) {
            versionSpec = "latest";
        }

        File nodeModule = new File(nodeModulesDir, module);
        boolean installPrivate = false;
        if (nodeModule.exists() ) {
            File targetDir = importingModuleDir.getName().equals("node_modules") ? nodeModulesDir:new File(importingModuleDir,"node_modules");
            String moduleKey = createModuleKey(module, targetDir);
            List<Promise> promises = packagesUnderway.get(moduleKey);
            if ( promises != null )
            {
                int debug = 1;
            }
            String finalVersionSpec1 = versionSpec;
            if ( promises!=null && promises.size() > 0 ) // timing: not unpacked
            {
                Log.Warn(this, "Delaying because in transfer:"+module+" "+targetDir.getAbsolutePath());
                Promise p = new Promise();
                delayed(1000, ()-> npmInstall(module, finalVersionSpec1, importingModuleDir).then(p) );
                return p;
            }
            File pack = new File(nodeModule,"package.json");
            if ( pack.exists() && versionSpec.indexOf(".") > 0 ) {
                String version = null;
                String vspec = null;
                try {
                    JsonObject pjson = Json.parse(new FileReader(pack)).asObject();
                    version = pjson.getString("version", null);
                    vspec = versionSpec;
                    if (!matches(version,vspec)) {
                        Log.Warn(this,"version mismatch for module '"+module+"'. requested:"+versionSpec+" from '"+importingModuleDir.getName()+"' installed:"+version+". (delete module dir for update)");
                        if ( config.getVersion(module) == null ) {
                            installPrivate = true;
                            Log.Warn(this,"   installing private "+module);
                        } else {
                            Log.Warn(this,"   stick with mismatch because of jnpm.kson config entry for "+module);
                        }
                    }
                } catch (Exception e) {
                    Log.Error(this, "can't parse package.json in "+module+" "+importingModuleDir.getAbsolutePath()+". Retry");
                    Promise p = new Promise();
                    delayed(1000, ()-> npmInstall(module, finalVersionSpec1, importingModuleDir).then(p) );
                    return p;
                }
            } else
                return resolve(InstallResult.EXISTS);
        }

        Promise p = new Promise();

        // fire in parallel
        IPromise<List<String>> versionProm = getVersions(module);
        IPromise<JsonObject> distributionsProm = getDistributions(module);

        String finalVersionSpec = versionSpec == null ? "latest" : versionSpec;
        boolean finalInstallPrivate = installPrivate;
        distributionsProm.then( (dist, derr) -> {
            if ( dist == null ) {
                dist = new JsonObject();
                Log.Error(this,"distribution is empty or error "+derr+" in module:"+module);
            }
            JsonObject finalDist = dist;

            versionProm.then( (versions, verr) -> {
                if ( versions == null ) {
                    p.reject(verr);
                    return;
                }
                String resolvedVersion = getVersion(module, finalVersionSpec, versions, finalDist);
                http.getContent(config.getRepo()+"/"+module+"/"+ resolvedVersion).then( (cont, err) -> {
                    if ( cont != null ) {
                        JsonObject pkg = Json.parse(cont).asObject();

                        String tarUrl = pkg.get("dist").asObject().get("tarball").asString();

                        JsonValue dependencies = pkg.get("dependencies");
                        if ( dependencies == null )
                            dependencies = new JsonObject();
                        else
                            dependencies = dependencies.asObject();

                        JsonValue peerdependencies = pkg.get("peerDependencies");
                        if (peerdependencies != null) {
                            ((JsonObject) dependencies).merge(peerdependencies.asObject());
                        }

                        File targetDir = finalInstallPrivate ? new File(importingModuleDir,"node_modules"):nodeModulesDir;
                        if ( finalInstallPrivate )
                            targetDir.mkdirs();
                        IPromise depP = downLoadAndInstall(tarUrl,module,resolvedVersion,targetDir);
                        List deps = new ArrayList<>();
                        deps.add(depP);
                        File importingDir = new File(nodeModulesDir,module);
                        ((JsonObject) dependencies).forEach( member -> {
                            deps.add(npmInstall(member.getName(),member.getValue().asString(),importingDir));
                        });
                        all(deps).then( (r,e) -> {
                            p.resolve(InstallResult.INSTALLED);
                        });
                    } else {
                        p.reject("no such module");
                    }
                });
            });
        });
        return p;
    }

    protected IPromise<List<String>> getVersions(String module) {
        Promise res = new Promise();
        http.getContent(config.getRepo()+"/"+module).then( (cont,err) -> {
            if ( cont != null ) {
                JsonObject parse = Json.parse(cont).asObject().get("versions").asObject();
                List<String> versions = new ArrayList<>();
                parse.forEach( mem -> versions.add(mem.getName()));
                res.resolve(versions);
            } else {
                res.reject(err);
            }
        });
        return res;
    }

    // map of tag => version
    protected IPromise<JsonObject> getDistributions(String module) {
        Promise res = new Promise();
//        http://registry.npmjs.org/-/package/react/dist-tags
        http.getContent(config.getRepo()+"/-/package/"+module+"/dist-tags").then( (cont,err) -> {
            if ( cont != null ) {
                JsonObject parse = Json.parse(cont).asObject();
                res.resolve(parse);
            } else {
                res.reject(err);
            }
        });
        return res;
    }

    Map<String,List<Promise>> packagesUnderway = new HashMap<>();
    private IPromise downLoadAndInstall(String tarUrl, String moduleName, String resolvedVersion, File targetDir) {
        Promise p = new Promise();
        String moduleKey = createModuleKey(moduleName,targetDir);
        List<Promise> promlist = packagesUnderway.get(moduleKey);
        if (promlist!=null) {
            if ( promlist.size() == 0 ) {
                // timing: has already arrived and proms resolved
                p.resolve(true);
            } else {
                packagesUnderway.get(moduleKey).add(p);
            }
            return p;
        }
        Log.Info(this,String.format("installing '%s' in %s.", moduleName +"@"+ resolvedVersion, targetDir.getAbsolutePath()));
        ArrayList list = new ArrayList();
        packagesUnderway.put(moduleKey, list);
        list.add(p);
        checkThread();
        http.getContentBytes(tarUrl).then( (resp,err) -> {
            execInThreadPool( () -> { // multithread unpacking (java io blocks, so lets mass multithread)
                byte b[] = resp;
                try {
                    b = AsyncHttpActor.unGZip(b,b.length*10);
                    File outputDir = new File(targetDir, moduleName);
                    unTar(new ByteArrayInputStream(b), outputDir);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                } catch (ArchiveException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }).then( () -> {
                checkThread();
//                Log.Info(this,String.format("installed '%s' in %s.", moduleName+"@"+ resolvedVersion, nodeModulesDir.getAbsolutePath()));
                packagesUnderway.get(moduleKey).forEach(prom -> prom.resolve(true) );
                packagesUnderway.get(moduleKey).clear();
            });
        });
        return p;
    }

    private String createModuleKey(String moduleName, File targetDir) {
        try {
            return moduleName+"#"+targetDir.getCanonicalPath();
        } catch (IOException e) {
            return moduleName+"#"+targetDir.getAbsolutePath();
        }
    }

    private static List<File> unTar(final InputStream is, final File outputDir) throws FileNotFoundException, IOException, ArchiveException {
        final List<File> untaredFiles = new LinkedList<File>();
        final TarArchiveInputStream debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", is);
        TarArchiveEntry entry = null;
        while ((entry = (TarArchiveEntry)debInputStream.getNextEntry()) != null) {
            String name = entry.getName();
            if ( name.startsWith("package/") )
                name = name.substring("package/".length());
            final File outputFile = new File(outputDir, name);
            if (entry.isDirectory()) {
                if (!outputFile.mkdirs()) {
                    throw new IllegalStateException(String.format("Couldn't create directory %s.", outputFile.getAbsolutePath()));
                }
            } else {
                outputFile.getParentFile().mkdirs();
                final OutputStream outputFileStream = new FileOutputStream(outputFile);
                IOUtils.copy(debInputStream, outputFileStream);
                outputFileStream.close();
            }
            untaredFiles.add(outputFile);
        }
        debInputStream.close();

        return untaredFiles;
    }

    static JNPM singleton;
    public static IPromise<InstallResult> Install(String module, String versionSpec, File modulesDir, JNPMConfig config) {
        Promise p = new Promise();
        synchronized ( JNPM.class ) {
            if ( singleton == null || singleton.isStopped() ) {
                singleton = AsActor(JNPM.class, AsyncHttpActor.getSingleton().getScheduler());
                singleton.init(modulesDir, config);
            }
        }
        singleton.npmInstall(module, versionSpec, modulesDir).then((r, e) -> {
//            Log.Info(JNPM.class,"DONE "+r+" "+e);
//                unpkgCrawler.stop();
            p.complete(r, e);
        });
        return p;
    }

    public static void main(String[] args) {
        boolean matches = matches("15.6.1", ">=0.14.0 <= 15");
        matches = matches("15.6.1", ">=0.14.0 <=15");
        matches = matches("16.0.0-beta.5", "^15.6.1");
        System.out.println(matches("15.6.2","^0.14.9 || >=15.3.0"));
        if ( 1 != 0 )
            return;
        InstallResult res = Install(
            "react-dom",
            null,
            new File("./modules/kontraktor-http/src/test/node_modules"),
            JNPMConfig.read("./modules/kontraktor-http/src/test/jnpm.kson")
        ).await();
        System.out.println("DONE "+res);
    }
    //http://registry.npmjs.org/-/package/react/dist-tags

}
