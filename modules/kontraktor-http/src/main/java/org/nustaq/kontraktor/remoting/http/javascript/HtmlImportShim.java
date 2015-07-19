package org.nustaq.kontraktor.remoting.http.javascript;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.nustaq.kontraktor.remoting.http.javascript.jsmin.JSMin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by ruedi on 16/07/15.
 */
public class HtmlImportShim {

    public interface ResourceLocator {
        File locateResource( String urlPath );
    }

    boolean inlineCss = true;
    boolean inlineScripts = true;
    boolean stripComments = true;
    boolean inlineHtml;
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

    public HtmlImportShim inlineHtml(boolean inlineHtml) {
        this.inlineHtml = inlineHtml;
        return this;
    }


    public HtmlImportShim(String baseUrl) {
        this.baseUrl = new KUrl(baseUrl);
    }

    public File locateResource( KUrl urlPath ) {
        File file = locator.locateResource( baseUrl.concat(urlPath).toUrlString() );
        if ( file == null || ! file.exists() ) {
            System.out.println("failed to resolve '"+urlPath+"'");
        }
        return file;
    }

    public void setLocator(DependencyResolver locator) {
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
            if ( minify && inlineScripts && scripts != null ) {
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
        if ("import".equals(rel) ) {
            if ( inlineHtml && type.indexOf("html") >= 0 ) {
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
                                module.attr("assetpath", baseUrl.concat(assetPath).toUrlString() );
                            });

                            imp.getElementsByTag("head").forEach(node -> {
                                List<Node> children = new ArrayList<>(node.children());
                                // children.add(0, new Comment(" == "+impFi.getName()+" == ",""));
                                changes.add(() -> {
                                    Integer integer = link.siblingIndex();
                                    if ( containingFileUrl.toUrlString().equals("index.html")) {
                                        int debug = 1;
                                    }
                                    link.parent().insertChildren(integer, children);
                                });
                            });
                            final List<List<Node>> finalBodyContent = bodyContent;
                            imp.getElementsByTag("body").forEach(node -> {
                                finalBodyContent.add(new ArrayList<>(node.children()));
                            });
                            changes.add( () -> link.remove() );
                            inlineScripts(impUrl, visited, changes, imp);
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
            } else if ( inlineCss && ("stylesheet".equals(type) || "css".equals(type)) ) {
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
        } // import link
    }

    public void inlineScripts(KUrl containingFileUrl, HashSet<KUrl> visited, List<Runnable> changes, Element doc) throws IOException {
        if ( inlineScripts ) {
            Elements scripts = doc.getElementsByTag("script");
            inlineScripts(containingFileUrl,visited,changes,scripts);
        }
    }

    public void inlineScripts(KUrl containingFileUrl, HashSet<KUrl> visited, List<Runnable> changes, Elements scripts) throws IOException {
        if ( inlineScripts ) {
            for (int i = 0; i < scripts.size(); i++) {
                Element script = scripts.get(i);
                String href = script.attr("src");
                String ignore = script.attr("no-inline");
                if ( ignore != "" ) {
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
                                visited.add(url);
                                Element style = new Element(Tag.valueOf("script"), "" );
                                byte[] bytes = Files.readAllBytes(impFi.toPath());
                                if ( minify && url.getExtension().equals("js") )
                                    bytes = JSMin.minify(bytes);
                                style.appendChild(new DataNode(new String(bytes, "UTF-8"), ""));
                                changes.add(() -> script.replaceWith(style));
                            }
                        }
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

    public HtmlImportShim inlineScripts(final boolean inlineScripts) {
        this.inlineScripts = inlineScripts;
        return this;
    }

    public HtmlImportShim inlineCss(final boolean inlineCss) {
        this.inlineCss = inlineCss;
        return this;
    }


    public static void main(String[] args) throws IOException {

        File file = new File("/home/ruedi/projects/polystrene/bower_components/paper-slider/paper-slider.html");
        File baseDir = new File("/home/ruedi/projects/polystrene/bower_components/");
        HtmlImportShim shim = new HtmlImportShim(baseDir, "");
        Element element = shim.shimImports( new KUrl("paper-slider/paper-slider.html"), new HashSet<>(), null);
        System.out.println(element);
    }

}
