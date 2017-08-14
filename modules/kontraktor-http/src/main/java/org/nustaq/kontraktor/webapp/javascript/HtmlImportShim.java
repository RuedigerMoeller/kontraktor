package org.nustaq.kontraktor.webapp.javascript;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.nustaq.kontraktor.webapp.javascript.jsmin.JSMin;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.util.FSTUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by ruedi on 16/07/15.
 *
 * inline all html imports of given file recursively (incl. adaption of pathes)
 * optionally also minify javascript code on the fly.
 * also inlines css files
 *
 * similar to polymer's vulcanize.
 *
 * by specifying 'no-inline=true' on a script tag one can exclude specific script tags
 *
 */
public class HtmlImportShim {

    public interface ResourceLocator {
        File locateResource( String urlPath );

        default byte[] retrieveBytes(File impFi) {
            try {
                return Files.readAllBytes(impFi.toPath());
            } catch (IOException e) {
                FSTUtil.rethrow(e);
            }
            return null;
        }
    }

    boolean inline = true;
    boolean stripComments = true;
    boolean minify = true;

    KUrl baseUrl;
    ResourceLocator locator;

    public HtmlImportShim( File baseDir, String baseUrl) {
        this.baseUrl = new KUrl(baseUrl);
        this.locator = new ResourceLocator() {
            @Override
            public File locateResource(String urlPath) {
                return new File(baseDir+"/"+urlPath );
            }
        };
    }

    public HtmlImportShim inline(boolean inline) {
        this.inline = inline;
        return this;
    }

    public HtmlImportShim(String baseUrl) {
        this.baseUrl = new KUrl(baseUrl);
    }

    public File locateResource( KUrl urlPath ) {
        File file = locator.locateResource( urlPath.toUrlString() );
        if ( file == null || ! file.exists() ) {
            Log.Warn(this, "failed to resolve '" + urlPath + "'");
        }
        return file;
    }

    public void setLocator(ResourceLocator locator) {
        this.locator = locator;
    }

    public Element shimImports(String htmlFile) throws IOException {
        return shimImports(new KUrl(htmlFile), new HashSet<>(), null);
    }

    protected KUrl computeAssetPath(KUrl containingFileUrl, String href) {
        return containingFileUrl.getParentURL().concat(href);
    }

    public Element shimImports(KUrl containingFileUrl, HashSet<KUrl> visited, List<List<Node>> bodyContent ) throws IOException {
        boolean isTop = false;
        if ( bodyContent == null ) {
            isTop = true;
            bodyContent = new ArrayList<>();
        }
        if ( visited.contains( containingFileUrl ) )
            return null;//new Element(Tag.valueOf("span"),"").html("<!-- "+htmlFile.getName()+"-->");
        visited.add(containingFileUrl);
        String compName = containingFileUrl.getFileNameNoExtension();
        File fi = locateResource(containingFileUrl);
        if ( fi == null ) {
            System.out.println("cannot locate "+containingFileUrl);
            return null;
        }
        Document doc = Jsoup.parse(fi, "UTF-8", baseUrl.toUrlString() );
        Elements scripts = null;
        if ( isTop ) {
            scripts = doc.getElementsByTag("script");
        }
        if ( stripComments ) {
            stripComments(doc);
        }
        List<Runnable> changes = new ArrayList<>();

        Elements links = doc.getElementsByTag("link");
        for (int i = 0; i < links.size(); i++) {
            Element link = links.get(i);
            shimLink( containingFileUrl, visited, bodyContent, changes, link);
        }

        // apply changes after analysis to avoid errors by concurrent iteration
        changes.forEach(change -> change.run());
        changes.clear();

        if ( inline ) {
            // adjust links to resources
            // fixme: misc links to component local resources ?
            // fixme: needs to respect individual inline flags separately
            Elements resourcLinks = doc.getElementsByAttribute("src");
            for (int i = 0; i < resourcLinks.size(); i++) {
                Element element = resourcLinks.get(i);
                if ( element.tagName().equals("img") || element.tagName().equals("embed") || element.tagName().equals("iframe") ) {
                    if ( element.attr("no-inline").equals("")) {
                        String src = element.attr("src");
                        KUrl linkUrl = new KUrl(src);
                        boolean symbolic =  src.startsWith("{{");
                        if ( symbolic ) {
                            int debug = 1;
                        }
                        if ( linkUrl.isRelative() && !symbolic ) {
                            linkUrl = linkUrl.prepend( containingFileUrl.getParentURL().getName() );
                            element.attr("src", linkUrl.toUrlString());
                        }
                    }
                }
            }
        }

//        if ( inline ) {
//            // actually scripts should be empty, re-remove them
//            scripts = doc.getElementsByTag("script");
//            for (int i = 0; i < scripts.size(); i++) {
//                Element script = scripts.get(i);
//                if ( script.hasAttr("src") && ! script.attr("src").startsWith("http") && ! script.hasAttr("no-inline") ) {
//                    script.remove();
//                }
//            }
//        }

        if ( isTop ) {
            Element body = doc.getElementsByTag("body").first();
            Element div = new Element(Tag.valueOf("div"),"");
            div.attr("hidden","");
            div.attr("by-vulcanize","");
            div.attr("not-really","");
            for (int i = bodyContent.size()-1; i >= 0; i--) {
                List<Node> children = bodyContent.get(i);
                div.insertChildren(0,children);
            }
            ArrayList<Node> children = new ArrayList<>();
            children.add(div);
            body.insertChildren(0, children);
            if ( inline && scripts != null ) {
                inlineScripts(containingFileUrl, visited, changes, scripts );
                changes.forEach( change -> change.run() );
                changes.clear();
            }

        }
        return doc;
    }

    public void shimLink(KUrl containingFileUrl, HashSet<KUrl> visited, List<List<Node>> bodyContent, List<Runnable> changes, Element link) throws IOException {
        String rel = link.attr("rel");
        String type = link.attr("type");
        String ignore = link.attr("no-inline");
        if ( ignore != "" ) {
            return;
        }
        if ( "import".equals(rel) ) {
            if ( type == null || type.length() == 0 ) {
                String href = link.attr("href");
                if ( href == null || href.length() == 0 ) {
                    type ="";
                } else {
                    type = href.substring(href.lastIndexOf('.')+1);
                }
            }
        }
        if ( link.attr("href").indexOf("skeleton") >= 0 ) {
            int debug = 1;
        }
        if ( inline && type.indexOf("html") >= 0 && "import".equals(rel)) {
            String href = link.attr("href");
            String noinline = link.attr("no-inline");
            if ( noinline != "" ) {
                // do nothing
            } else if ( !href.startsWith("http") ) {
                try {
                    KUrl impUrl = containingFileUrl.getParentURL().concat(href);
                    Element imp = shimImports(impUrl, visited, bodyContent);
                    if (imp instanceof Document) {
                        KUrl assetPath = computeAssetPath(containingFileUrl,href); //FIXME: needs to be computed to initial dir, will work for level 1 only
                        imp.getElementsByTag("dom-module").forEach( module -> {
                            module.attr("assetpath", baseUrl.concat(assetPath).toUrlString());
                        });

                        imp.getElementsByTag("head").forEach(node -> {
                            List<Node> children = new ArrayList<>(node.children());
                            // children.add(0, new Comment(" == "+impFi.getName()+" == ",""));
                            changes.add(() -> {
                                Integer integer = link.siblingIndex();
                                link.parent().insertChildren(integer, children);
                            });
                        });
                        inlineScripts(impUrl, visited, changes, imp);
                        final List<List<Node>> finalBodyContent = bodyContent;
                        changes.add(() -> {
                            imp.getElementsByTag("body").forEach(node -> {
                                finalBodyContent.add(new ArrayList<>(node.children()));
                            });
                        });
                        changes.add( () -> link.remove() );
                    } else {
                        if ( imp == null ) {
                            changes.add(() -> link.remove() );
                        }
                        else {
                            final Element finalImp = imp;
                            changes.add(() -> link.replaceWith(finalImp) );
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if ( inline && ("stylesheet".equals(type) || "css".equals(type) || rel.equals("stylesheet")) ) {
            String href = link.attr("href");
            String noinline = link.attr("no-inline");
            if ( noinline != "" ) {
            } else if ( href != null && ! href.startsWith("http") ) {
                KUrl resUrl = containingFileUrl.getParentURL().concat(href);
                File impFi = locateResource(resUrl);
                if ( impFi != null && impFi.exists() ) {
                    if ( visited.contains(resUrl) ) {
                        link.remove();
                    } else {
                        visited.add(resUrl);
                        Element style = new Element(Tag.valueOf("style"), "" );
                        byte[] bytes = Files.readAllBytes(impFi.toPath());
                        style.appendChild( new DataNode(new String(bytes,"UTF-8"),"") );
                        link.replaceWith(style);
                    }
                }
            }
        }
    }

    public void inlineScripts(KUrl containingFileUrl, HashSet<KUrl> visited, List<Runnable> changes, Element doc) throws IOException {
        if ( inline ) {
            Elements scripts = doc.getElementsByTag("script");
            inlineScripts(containingFileUrl,visited,changes,scripts);
        }
    }

    public void inlineScripts(KUrl containingFileUrl, HashSet<KUrl> visited, List<Runnable> changes, Elements scripts) throws IOException {
        if ( inline ) {
            for (int i = 0; i < scripts.size(); i++) {
                Element script = scripts.get(i);
                String href = script.attr("src");
                boolean ignore = script.hasAttr("no-inline");
                if ( ignore ) {
                    continue;
                }
                if ( href != null && href.length() > 0 ) {
                    if ( !href.startsWith("http") ) {
                        KUrl url = containingFileUrl.getParentURL().concat(href);
                        File impFi = locateResource(url);
                        if ( impFi != null && impFi.exists() ) {
                            if ( visited.contains(url) ) {
                                changes.add(() -> script.remove());
                            } else {
                                Log.Info(this, "inlining script " + href);
                                visited.add(url);
                                Element newScript = new Element(Tag.valueOf("script"), "" );
                                byte[] bytes = locator.retrieveBytes(impFi);
                                if ( minify && url.getExtension().equals("js") )
                                    bytes = JSMin.minify(bytes);
                                String scriptSource = new String(bytes, "UTF-8");
                                newScript.appendChild(new DataNode(scriptSource, ""));
                                newScript.attr("no-inline", "true");
                                changes.add(() -> script.replaceWith(newScript));
                            }
                        }
                    }
                } else {
                    if ( minify && ! script.hasAttr("no-inline")) {
                        String minified = new String(JSMin.minify(script.html().getBytes("UTF-8")), "UTF-8");
                        Element newScript = new Element(Tag.valueOf("script"), "" );
                        newScript.appendChild(new DataNode(minified, ""));
                        newScript.attr("no-inline", "true");
                        changes.add(() -> script.replaceWith(newScript));
                    }
                }
            }
        }
    }

    public void stripComments(Document doc) {
        List<Node> comments = new ArrayList<>();
        doc.getAllElements().forEach( elem -> {
            if ( ! elem.tagName().equals("style") && ! elem.equals("script") ) {
                elem.childNodes().forEach( child -> {
                    if ( child instanceof Comment) {
                        comments.add(child);
                    }
                });
            }
        });

        comments.forEach(node -> node.remove());
    }

    public KUrl getBaseUrl() {
        return baseUrl;
    }

    public HtmlImportShim stripComments(final boolean stripComments) {
        this.stripComments = stripComments;
        return this;
    }

    public HtmlImportShim minify(final boolean minify) {
        this.minify = minify;
        return this;
    }


    public static void main(String[] args) throws IOException {

        File file = new File("/home/ruedi/projects/mnistplay/uberholer/website/index.html");
        File baseDir = new File("/home/ruedi/projects/mnistplay/uberholer/website/");

        HtmlImportShim shim = new HtmlImportShim(baseDir, "");
        Element element = shim.shimImports( new KUrl("index.html"), new HashSet<>(), null);
        PrintStream ps = new PrintStream(new FileOutputStream("/home/ruedi/projects/mnistplay/uberholer/dist/index.html"));
        ps.println(element);
        ps.close();
    }

}
