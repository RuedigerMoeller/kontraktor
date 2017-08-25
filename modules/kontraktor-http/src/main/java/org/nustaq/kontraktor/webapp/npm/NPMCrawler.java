package org.nustaq.kontraktor.webapp.npm;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.github.zafarkhaja.semver.Version;
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

    File nodeModulesDir;
    AsyncHttpActor http;

    public void init(File nodeModules) {
        nodeModulesDir = nodeModules;
        http = AsyncHttpActor.getSingleton();
    }

    public static enum InstallResult {
        EXISTS,
        INSTALLED,
        NOT_FOUND,
        FAILED
    }

    protected String getVersion(String module, String spec, List<String> versions, JsonObject finalDist) {
        if ( spec == null ) {
            return "latest";
        }
        try {
            Version version = Version.valueOf(spec);
        } catch (Exception e) {
//            if ( versions.getString(spec,"latest") != null ) {
//                return versions.getString(spec,"latest");
//            }
        }
        return "latest";
    }

    public IPromise<InstallResult> npmInstall( String module, String versionSpec ) {
        File file = new File(nodeModulesDir, module);
        if (file.exists() ) {
            return resolve(InstallResult.EXISTS);
        }
        Promise p = new Promise();

        // fire in parallel
        IPromise<List<String>> versionProm = getVersions(module);
        IPromise<JsonObject> distributionsProm = getDistributions(module);

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
                http.getContent(repo+module+"/"+getVersion(module,versionSpec,versions, finalDist)).then( (cont, err) -> {
                    if ( cont != null ) {
                        JsonObject pkg = Json.parse(cont).asObject();

                        String tarUrl = pkg.get("dist").asObject().get("tarball").asString();

                        JsonValue dependencies = pkg.get("dependencies");
                        if ( dependencies == null )
                            dependencies = new JsonObject();
                        else
                            dependencies = dependencies.asObject();

                        downLoadAndInstall(tarUrl,module);
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
    private IPromise downLoadAndInstall(String tarUrl, String moduleName) {
        Promise p = new Promise();
        if ( packagesUnderway.containsKey(moduleName) ) {
            // FIXME: trigger but no finished yet
            packagesUnderway.get(moduleName).add(p);
            return p;
        }
        ArrayList list = new ArrayList();
        packagesUnderway.put(moduleName, list);
        list.add(p);
        http.getContentBytes(tarUrl).then( (resp,err) -> {
            exec( () -> { // multithread unpacking (java io blocks, so lets mass multithread)
                byte b[] = resp;
                try {
                    b = AsyncHttpActor.unGZip(b,b.length*10);
                    File outputDir = new File(nodeModulesDir, moduleName);
                    Log.Info(this,String.format("unpack %s.", outputDir.getAbsolutePath()));
                    unTar(new ByteArrayInputStream(b), outputDir);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ArchiveException e) {
                    e.printStackTrace();
                }
                return "dummy";
            }).then( () -> {
                packagesUnderway.get(moduleName).forEach( prom -> prom.resolve(true) );
            });
        });
        return p;
    }

    private static List<File> unTar(final InputStream is, final File outputDir) throws FileNotFoundException, IOException, ArchiveException {
        final List<File> untaredFiles = new LinkedList<File>();
        final TarArchiveInputStream debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", is);
        TarArchiveEntry entry = null;
        while ((entry = (TarArchiveEntry)debInputStream.getNextEntry()) != null) {
            final File outputFile = new File(outputDir, entry.getName());
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
        NPMCrawler unpkgCrawler = AsActor(NPMCrawler.class);
        unpkgCrawler.init(new File("/home/ruedi/IdeaProjects/kontraktor/modules/kontraktor-http/src/test/node_modules"));
        unpkgCrawler.npmInstall("react", null).then( (r,e) -> {
            System.out.println("DONE "+r+" "+e);
            unpkgCrawler.stop();
        });

        //http://registry.npmjs.org/-/package/react/dist-tags
    }
}
