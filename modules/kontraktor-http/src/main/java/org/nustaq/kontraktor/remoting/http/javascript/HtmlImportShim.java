package org.nustaq.kontraktor.remoting.http.javascript;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.nustaq.kontraktor.remoting.http.javascript.jsmin.JSMin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
    boolean minify = true;

    String baseUrl;
    ResourceLocator locator;

    public HtmlImportShim( File baseDir, String baseUrl) {
        this.baseUrl = baseUrl;
        this.locator = new ResourceLocator() {
            @Override
            public File locateResource(String urlPath) {
                return new File(normalizeUrl(baseDir+"/"+urlPath));
            }
        };
    }

    public HtmlImportShim(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String normalizeUrl( String url ) {
        return DependencyResolver.stripDoubleSeps(url);
    }

    public File locateResource( String urlPath ) {
        File file = locator.locateResource(normalizeUrl(baseUrl + "/" + urlPath));
        if ( file == null || ! file.exists() ) {
            System.out.println("failed to resolve '"+urlPath+"'");
        }
        return file;
    }

    public String getName( String url ) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    public String getParentUrl( String url ) {
        int idx = url.lastIndexOf("/");
        if ( idx >= 0 )
            return url.substring(0, idx);
        return "";
    }

    public String getCompName( String url ) {
        String name = getName(url);
        int idx = name.lastIndexOf(".");
        if ( idx >= 0 ) {
            name = name.substring(0,idx);
        }
        return name;
    }

    public void setLocator(DependencyResolver locator) {
        this.locator = locator;
    }

    protected String resolveLinkToUrl(String containingFileUrl, String href) {
        if ( href.contains("marked.js") ) {
            System.out.println("POK");
        }
        String parent = getParentUrl(containingFileUrl);
        return parent+"/"+href;
    }

    public Element shimImports(String htmlFile) throws IOException {
        return shimImports(htmlFile,new HashSet<>(), null);
    }

    protected String computeAssetPath(String containingFileUrl, String href) {
        String parent = getParentUrl(containingFileUrl);
        return parent+"/"+href;
    }

    public Element shimImports(String fileUrl, HashSet<String> visited, List<List<Node>> bodyContent ) throws IOException {
        boolean isTop = false;
        if ( bodyContent == null ) {
            isTop = true;
            bodyContent = new ArrayList<>();
        }
        if ( visited.contains( unify(fileUrl) ) )
            return null;//new Element(Tag.valueOf("span"),"").html("<!-- "+htmlFile.getName()+"-->");
        visited.add( unify(fileUrl) );
        String compName = getCompName(fileUrl);
        File fi = locateResource(fileUrl);
        if ( fi == null ) {
            System.out.println("cannot locate "+fileUrl);
            return null;
        }
        Document doc = Jsoup.parse(fi, "UTF-8", baseUrl);
        if ( stripComments ) {
            stripComments(doc);
        }
        List<Runnable> changes = new ArrayList<>();

        Elements links = doc.getElementsByTag("link");
        for (int i = 0; i < links.size(); i++) {
            Element link = links.get(i);
            //if (link.parent().tagName().equals("head") || true)
            {
                shimLink( fileUrl, visited, bodyContent, changes, link);
            }
        }
        // apply changes after analysis to avoid errors by concurrent iteration
        changes.forEach( change -> change.run() );
        changes.clear();

        if ( isTop ) {
            if ( inlineScripts ) {
                Elements scripts = doc.getElementsByTag("script");
                for (int i = 0; i < scripts.size(); i++) {
                    Element script = scripts.get(i);
                    String href = script.attr("src");
                    if ( href != null && href.length() > 0 ) {
                        if ( !href.startsWith("http") ) {
                            String url = resolveLinkToUrl(fileUrl,href);
                            File impFi = locateResource(url);
                            if ( impFi != null && impFi.exists() ) {
                                if ( visited.contains(unify(url)) ) {
                                    changes.add(() -> script.remove());
                                } else {
                                    visited.add(unify(url));
                                    Element style = new Element(Tag.valueOf("script"), "" );
                                    byte[] bytes = Files.readAllBytes(impFi.toPath());
                                    style.appendChild(new DataNode(new String(bytes, "UTF-8"), ""));
                                    changes.add(() -> script.replaceWith(style));
                                }
                            }
                        }
                    }
                }
                changes.forEach( change -> change.run() );
                changes.clear();
            }

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
            if ( minify ) {
                Elements scripts = doc.getElementsByTag("script");
                for (int i = 0; i < scripts.size(); i++) {
                    Element script = scripts.get(i);
                    String href = script.attr("src");
                    if ( href == null || href.length() == 0 ) {
                        String scriptString = script.html();
                        byte[] minified = minify(scriptString.getBytes("UTF-8"));

                        Element newScript = new Element(Tag.valueOf("script"), "" );
                        newScript.appendChild(new DataNode(new String(minified, "UTF-8"), ""));
                        changes.add(() -> script.replaceWith(newScript));
                    }
                }
                changes.forEach( change -> change.run() );
                changes.clear();
            }

        }
        return doc;
    }

    private String unify(String fileUrl) {
        return getName(fileUrl);
    }

    public void shimLink(String containingFileUrl, HashSet<String> visited, List<List<Node>> bodyContent, List<Runnable> changes, Element link) throws IOException {
        String rel = link.attr("rel");
        String type = link.attr("type");
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
            if ( type.indexOf("html") >= 0 ) {
                String href = link.attr("href");
                if ( !href.startsWith("http") ) {
                    try {
                        String impUrl = resolveLinkToUrl(containingFileUrl, href);
                        Element imp = shimImports(impUrl, visited, bodyContent);
                        if (imp instanceof Document) {
                            String assetPath = computeAssetPath(containingFileUrl,href); //FIXME: needs to be computed to initial dir, will work for level 1 only
                            imp.getElementsByTag("dom-module").forEach( module -> {
                                module.attr("assetpath", baseUrl+assetPath );
                            });

                            imp.getElementsByTag("head").forEach(node -> {
                                List<Node> children = new ArrayList<>(node.children());
                                // children.add(0, new Comment(" == "+impFi.getName()+" == ",""));
                                changes.add(() -> {
                                    Integer integer = link.elementSiblingIndex();
                                    link.parent().insertChildren(integer, children);
                                });
                            });
                            final List<List<Node>> finalBodyContent = bodyContent;
                            imp.getElementsByTag("body").forEach(node -> {
                                finalBodyContent.add(new ArrayList<>(node.children()));
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
            } else if ( inlineCss && ("stylesheet".equals(type) || "css".equals(type)) ) {
                String href = link.attr("href");
                if ( href != null && ! href.startsWith("http") ) {
                    String resUrl = resolveLinkToUrl(containingFileUrl,href);
                    File impFi = locateResource(resUrl);
                    if ( impFi != null && impFi.exists() ) {
                        if ( visited.contains(unify(resUrl)) ) {
                            link.remove();
                        } else {
                            visited.add(unify(resUrl));
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

    public byte[] minify( byte bytes[] ) {
        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream bout = new ByteArrayOutputStream(bytes.length);
        JSMin.builder().inputStream(bin).outputStream(bout).build().minify();
        return bout.toByteArray();
    }

    public String getBaseUrl() {
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
        Element element = shim.shimImports("paper-slider/paper-slider.html", new HashSet<>(), null);
        System.out.println(element);
    }

}
