package org.nustaq.kontraktor.webapp.npm;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.github.jknack.semver.Semver;
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

public class NPMCrawler extends Actor<NPMCrawler> {

    public static String repo = "http://registry.npmjs.org/";
    public static enum InstallResult {
        EXISTS,
        INSTALLED,
        NOT_FOUND,
        FAILED
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
        spec = patchVSpec(spec);
        try {
            Semver semverMatcher = Semver.create(spec);
            versions.forEach(vers -> {
                try {
                    if (matches(semverMatcher,vers))
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

    private boolean matches(Semver sem, String version) {
        int i = version.indexOf("-");
        if ( i > 0 ) {
            version = version.substring(0,i);
        }
        return sem.matches(version);
    }

    private String patchVSpec(String spec) {
        if ( spec.startsWith("^") ) {
            String oldspec = spec;
            String rawversion = oldspec.substring(1);
            String[] split = rawversion.split("\\.");
            int major = Integer.parseInt(split[0]);
            spec = ">="+ rawversion +" < "+(major+1)+".0.0";
        }
        return spec.replace('^','~'); // FIXME: suboptimal, workaround semver bug
    }

    public IPromise<InstallResult> npmInstall( String module, String versionSpec ) {
        if (versionSpec == null) {
            versionSpec = "latest";
        }
        if (packagesUnderway.containsKey(module) ) {
            return resolve(InstallResult.INSTALLED);
        }

        File nodeModule = new File(nodeModulesDir, module);
        if (nodeModule.exists() ) {
            File pack = new File(nodeModule,"package.json");
            if ( pack.exists() && versionSpec.indexOf(".") > 0 ) {
                String version = null;
                String vspec = null;
                try {
                    JsonObject pjson = Json.parse(new FileReader(pack)).asObject();
                    version = pjson.getString("version", null);
                    vspec = patchVSpec(versionSpec);
                    if (!matches(Semver.create(vspec),version)) {
                        Log.Warn(this,"version mismatch for module '"+module+"'. requested:"+versionSpec+" installed:"+version+". (delete module dir for update)");
                    }
                } catch (Exception e) {
                    System.out.println("VERSION:"+version+" SPEC:"+vspec+" module:"+module);
                    e.printStackTrace();
                }
            }
            return resolve(InstallResult.EXISTS);
        }
        Promise p = new Promise();

        // fire in parallel
        IPromise<List<String>> versionProm = getVersions(module);
        IPromise<JsonObject> distributionsProm = getDistributions(module);

        String finalVersionSpec = versionSpec == null ? "latest" : versionSpec;
        distributionsProm.then( (dist, derr) -> {
            if ( dist == null ) {
                dist = new JsonObject();
                Log.Warn(this,"distribution is empty or error "+derr);
            }
            JsonObject finalDist = dist;

            versionProm.then( (versions, verr) -> {
                if ( versions == null ) {
                    p.reject(verr);
                    return;
                }
                String resolvedVersion = getVersion(module, finalVersionSpec, versions, finalDist);
                http.getContent(repo+module+"/"+ resolvedVersion).then( (cont, err) -> {
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

                        downLoadAndInstall(tarUrl,module,resolvedVersion);
                        List deps = new ArrayList<>();
                        ((JsonObject) dependencies).forEach( member -> {
                            deps.add(npmInstall(member.getName(),member.getValue().asString()));
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
        http.getContent(repo+module).then( (cont,err) -> {
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
        http.getContent(repo+"-/package/"+module+"/dist-tags").then( (cont,err) -> {
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
    private IPromise downLoadAndInstall(String tarUrl, String moduleName, String resolvedVersion) {
        Promise p = new Promise();
        if ( packagesUnderway.containsKey(moduleName) ) {
            // FIXME: trigger but no finished yet
            packagesUnderway.get(moduleName).add(p);
            return p;
        }
        Log.Info(this,String.format("installing '%s' in %s.", moduleName+"@"+ resolvedVersion, nodeModulesDir.getAbsolutePath()));
        ArrayList list = new ArrayList();
        packagesUnderway.put(moduleName, list);
        list.add(p);
        http.getContentBytes(tarUrl).then( (resp,err) -> {
            exec( () -> { // multithread unpacking (java io blocks, so lets mass multithread)
                byte b[] = resp;
                try {
                    b = AsyncHttpActor.unGZip(b,b.length*10);
                    File outputDir = new File(nodeModulesDir, moduleName);
                    unTar(new ByteArrayInputStream(b), outputDir);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ArchiveException e) {
                    e.printStackTrace();
                }
                return "dummy";
            }).then( () -> {
                packagesUnderway.get(moduleName).forEach( prom -> prom.resolve(true) );
                packagesUnderway.get(moduleName).clear();
            });
        });
        return p;
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

    public static void main(String[] args) {
        NPMCrawler unpkgCrawler = AsActor(NPMCrawler.class,AsyncHttpActor.getSingleton().getScheduler());
        File nodeModules = new File("./modules/kontraktor-http/src/test/node_modules");
        System.out.println(nodeModules.getAbsolutePath());
        unpkgCrawler.init(
            nodeModules,
            JNPMConfig.read("./modules/kontraktor-http/src/test/jnpm.kson") );
        unpkgCrawler.npmInstall("semantic-ui-react", null).then( (r,e) -> {
            Log.Info(NPMCrawler.class,"DONE "+r+" "+e);
            unpkgCrawler.stop();
        });

        //http://registry.npmjs.org/-/package/react/dist-tags
    }
}
