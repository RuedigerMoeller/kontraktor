package org.nustaq.utils;

import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.ssl.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.entity.ContentBufferEntity;
import org.apache.http.nio.entity.ContentInputStream;
import org.apache.http.protocol.HttpContext;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.Log;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.zip.GZIPInputStream;

/**
 * wraps apache async client in order to simplify concurrent / async crawling / downloads etc.
 * actor-styled
 */
public class AsyncHttpActor extends Actor<AsyncHttpActor> {

    public static int MAX_CONN_PER_ROUTE = 30;
    public static int MAX_CONN_TOTAL = 500;

    private static AsyncHttpActor singleton;

    protected CloseableHttpAsyncClient asyncHttpClient;

    final static Header NO_CACHE = new Header() {
        @Override
        public String getName() {
            return "Cache-Control";
        }
        @Override
        public String getValue() {
            return "no-cache";
        }
        @Override
        public HeaderElement[] getElements() throws ParseException {
            return new HeaderElement[0];
        }
    };

    public static String readContentString(HttpResponse resp) throws IOException {
        org.apache.http.Header[] headers = resp.getHeaders("Content-Type");
        String charset = "UTF-8";
        if ( headers != null ) {
            for (int i = 0; i < headers.length; i++) {
                org.apache.http.Header header = headers[i];
                org.apache.http.HeaderElement[] elements = header.getElements();
                if (elements != null) {
                    for (int j = 0; j < elements.length; j++) {
                        org.apache.http.HeaderElement element = elements[j];
                        NameValuePair cs = element.getParameterByName("charset");
                        if ( cs != null ) {
                            charset = cs.getValue().toUpperCase();
                        }
                    }
                }
            }
        }
        String enc = null;
        headers = resp.getHeaders("Content-Encoding");
        if ( headers != null ) {
            for (int i = 0; i < headers.length; i++) {
                org.apache.http.Header header = headers[i];
                org.apache.http.HeaderElement[] elements = header.getElements();
                if (elements != null) {
                    for (int j = 0; j < elements.length; j++) {
                        org.apache.http.HeaderElement element = elements[j];
                        if ( enc != null )
                            Log.Error(AsyncHttpActor.class,"unexpected encoding header");
                        enc = element.getName();
                    }
                }
            }
        }

        byte[] bytes = readContentBytes(resp,enc);
        String first = new String(bytes,0,Math.min(bytes.length,2000));
        if ( first.trim().startsWith("<?xml") ) {
            int i = first.indexOf("encoding=\"");
            if ( i > 0 ) {
                int beginIndex = i + "encoding=\"".length();
                int endIndex = first.indexOf('\"',beginIndex);
                if ( endIndex > beginIndex )
                    charset = first.substring(beginIndex, endIndex).toUpperCase();
                int debug = 1;
            }
        } else {
            int idx = first.indexOf("charset=");
            if ( idx >= 0 ) // charset is inside html document
            {
                int beginIndex = idx + "charset=".length();
                int endIndex = beginIndex;
                while( first.charAt(endIndex) != ' ' && first.charAt(endIndex) != '"' && first.charAt(endIndex) != '\'' && first.charAt(endIndex) != '>' && first.charAt(endIndex) != '/')
                    endIndex++;
                if ( endIndex > beginIndex )
                    charset = first.substring(beginIndex, endIndex).toUpperCase();
            }
        }
        String s = new String(bytes, charset);
//        System.out.println(s);
        for (int n=0; n < s.length(); n++ ) {
            if ( s.charAt(n) == 150 ) {
                s = s.replace((char)150,'-');
                break;
            }
        }
        return s;
    }

    public static byte[] readContentBytes(HttpResponse resp, String enc) throws IOException {
        ContentBufferEntity entity = (ContentBufferEntity) resp.getEntity();
        ContentInputStream in = (ContentInputStream) entity.getContent();
        byte barr[] = new byte[in.available()];
        int read = in.read(barr);
        if ( "gzip".equals(enc) ) {
            barr = unGZip(barr, read);
        }
        in.close();
        return barr;
    }

    public static byte[] unGZip(byte[] barr, int read) throws IOException {
        GZIPInputStream gin = new GZIPInputStream(new ByteArrayInputStream(barr));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(read*10);
        IOUtils.copy(gin,baos);
        gin.close();
        barr = baos.toByteArray();
        return barr;
    }

    protected CloseableHttpAsyncClient getClient() {
        synchronized (AsyncHttpActor.class)
        {
            if (asyncHttpClient == null ) {
                SSLContext context = null;
                try {
                    context = SSLContext.getInstance("SSL");
                    context.init(null, new TrustManager[] {
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }
                    }, null);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (KeyManagementException e) {
                    e.printStackTrace();
                }
                asyncHttpClient = HttpAsyncClients.custom()
                    .setSSLContext(context)
//                    .setDefaultRequestConfig(
//                        RequestConfig.custom()
//                            .setConnectTimeout(10000)
//                            .setCircularRedirectsAllowed(false)
//                            .setSocketTimeout(15000)
//                            .setConnectionRequestTimeout(10000)
//                            .build()
//                    )
                    .setMaxConnPerRoute(maxConnPerRoute)
                    .setMaxConnTotal(maxConnTotal)
                    .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                    .setDefaultIOReactorConfig(
                        IOReactorConfig.custom()
                            .setIoThreadCount(16)
                            .setSoKeepAlive(true)
                            .setSoReuseAddress(true)
                            .setConnectTimeout( 10_000 )
                            .setSoTimeout( 15_000 )
                            .build()
                    ).build();
                asyncHttpClient.start();
            }
            return asyncHttpClient;
        }
    }

    public static AsyncHttpActor getSingleton() {
        synchronized (AsyncHttpActor.class) {
            if ( singleton == null )
                singleton = AsActor(AsyncHttpActor.class);
            return AsyncHttpActor.singleton;
        }
    }

    public IPromise sync() {
        return new Promise("dummy");
    }

    private String tryCleanUpUrl(String url )
    {
        try {
            String modifiedUrl = url;

            // clean up url with double protocol (eg:"http://http://www.merkur.de...")
            boolean containsHttp = modifiedUrl.indexOf("http://") >= 0;
            boolean containsHttps = modifiedUrl.indexOf("https://") >= 0;

            modifiedUrl = modifiedUrl.replaceAll("http://", "");
            modifiedUrl = modifiedUrl.replaceAll("https://", "");

            // to be continued ... eg. with cleanup urls with double domains detection..

            // recreate protocol..
            if (containsHttps) {
                modifiedUrl = "https://" + modifiedUrl;
            } else if(containsHttp) {
                modifiedUrl = "http://" + modifiedUrl;
            }

            return modifiedUrl;
        }catch (Throwable t )
        {
            Log.Warn(this, "error while cleanup url:" + url);
            t.printStackTrace();
            return url;
        }
    }

    int maxConnPerRoute = MAX_CONN_PER_ROUTE;
    int maxConnTotal = MAX_CONN_TOTAL;

    /**
     * overwrite static limits, WARNING: careful when accessing the singleton
     *
     * @param maxConPerRoute
     * @param maxConTotal
     */
    public void setLimits(int maxConPerRoute, int maxConTotal) {
        this.maxConnPerRoute = maxConPerRoute;
        this.maxConnTotal = maxConTotal;
        if ( asyncHttpClient != null ) {
            try {
                asyncHttpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            asyncHttpClient = null;
        }
    }

    public IPromise<String> getContent(String url, String ... headers ) {
        if ( url == null || url.trim().length() == 0 ) {
            return reject("invalid url");
        }
        if ( !url.startsWith("http") ) {
            url = "http://"+url;
        }
        int idx = url.indexOf("#");
        if ( idx > 0 ) {
            url = url.substring(0,idx);
        }
        Promise res = new Promise();
        try {
            get(url, headers).then((response, err) -> {
                if (err != null) {
                    res.reject(err);
                    return;
                }
                if (response.getStatusLine().getStatusCode() != 200) {
                    res.reject(response.getStatusLine().getStatusCode());
                    return;
                }
                try {
                    res.resolve(readContentString(response));
                } catch (Throwable e) {
                    Log.Warn(this,e);
                    res.reject(e);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            res.reject(t);
        }
        return res;
    }

    public IPromise<byte[]> getContentBytes(String url, String ... headers ) {
        if ( url == null || url.trim().length() == 0 ) {
            return reject("invalid url");
        }
        if ( !url.startsWith("http") ) {
            url = "http://"+url;
        }
        int idx = url.indexOf("#");
        if ( idx > 0 ) {
            url = url.substring(0,idx);
        }
        Promise res = new Promise();
        try {
            get(url, headers).then((response, err) -> {
                if (err != null) {
                    res.reject(err);
                    return;
                }
                if (response.getStatusLine().getStatusCode() != 200) {
                    res.reject(response.getStatusLine().getStatusCode());
                    return;
                }
                String enc = null;
                Header heads[] = response.getHeaders("Content-Encoding");
                if ( heads != null ) {
                    for (int i = 0; i < heads.length; i++) {
                        Header header = heads[i];
                        HeaderElement[] elements = header.getElements();
                        if (elements != null) {
                            for (int j = 0; j < elements.length; j++) {
                                HeaderElement element = elements[j];
                                if ( enc != null )
                                    Log.Error(AsyncHttpActor.class,"unexpected encoding header");
                                enc = element.getName();
                            }
                        }
                    }
                }

                try {
                    res.resolve(readContentBytes(response,enc));
                } catch (Throwable e) {
                    Log.Warn(this,e);
                    res.reject(e);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            res.reject(t);
        }
        return res;
    }

    public IPromise<HttpResponse> get( String url, String ... headers ) {
        Promise res = new Promise();
        if (url==null) {
            int debug = 1;
        }
        try {
            String cleanedUrl = tryCleanUpUrl(url );
            HttpGet req = new HttpGet( cleanedUrl );
            setHeaders(req, headers);
            beChrome(req);
            getClient().execute(req, new FutureCallback<HttpResponse>() {

                @Override
                public void completed(HttpResponse result) {
                    execute(() -> res.resolve(result)); // switch to actor thread
                }

                @Override
                public void failed(Exception ex) {
                    execute(() -> res.reject(ex)); // switch to actor thread
                }

                @Override
                public void cancelled() {
                    execute(() ->res.reject("cancelled")); // switch to actor thread
                }

            });
        } catch (Throwable th) {
            Log.Warn(this,"get fail "+th+" "+url);
            res.reject(th);
        }
        return res;
    }

    public IPromise<HttpResponse> post(String url, String postData, String ... headers) {
        Promise res = new Promise();
        if (url==null) {
            int debug = 1;
        }
        try {
            HttpPost req = new HttpPost(url);
            setHeaders(req, headers);
            beChrome(req);
            req.setEntity(new StringEntity(postData));
            getClient().execute(req, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse result) {
                    execute(() -> res.resolve(result)); // switch to actor thread
                }

                @Override
                public void failed(Exception ex) {
                    execute(() -> res.reject(ex)); // switch to actor thread
                }

                @Override
                public void cancelled() {
                    execute(() ->res.reject("cancelled")); // switch to actor thread
                }

            });
        } catch (Throwable th) {
            Log.Warn(this,"get fail "+th+" "+url);
            res.reject(th);
        }
        return res;
    }

    public IPromise<HttpResponse> postWithContext(String url, String postData, HttpContext ctx, String ... headers) {
        Promise res = new Promise();
        if (url==null) {
            int debug = 1;
        }
        try {
            HttpPost req = new HttpPost(url);
            setHeaders(req, headers);
            beChrome(req);
            req.setEntity(new StringEntity(postData));
            getClient().execute(req, ctx, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse result) {
                    execute(() -> res.resolve(result)); // switch to actor thread
                }

                @Override
                public void failed(Exception ex) {
                    execute(() -> res.reject(ex)); // switch to actor thread
                }

                @Override
                public void cancelled() {
                    execute(() ->res.reject("cancelled")); // switch to actor thread
                }

            });
        } catch (Throwable th) {
            Log.Warn(this,"get fail "+th+" "+url);
            res.reject(th);
        }
        return res;
    }

    private void setHeaders(HttpRequestBase req, String[] headers) {
        for (int i = 0; i < headers.length; i+=2) {
            if ( headers[i] != null && headers[i+1]!=null)
                req.setHeader(headers[i],headers[i+1]);
        }
    }

    static String chromeHeaders[] = {
        "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
//        "Accept-Encoding","gzip, deflate, sdch", FIXME: should translate transparently, can be configured but HOW (as always apache is overly complex)
        "Accept-Language","de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4",
//        "Cache-Control","max-age=0",
//            "Host","www.theregister.co.uk",
//        "Referer","http://www.theregister.co.uk/",
        "Upgrade-Insecure-Requests","0",
        "User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.2924.76 Safari/537.36"
    };

    private void beChrome(HttpRequestBase req) {
        setHeaders(req,chromeHeaders);
    }

    public static void main(String[] args) throws InterruptedException {
        AsyncHttpActor http = Actors.AsActor(AsyncHttpActor.class);
        http.getContent("https://www.chefkoch.de/rs/s0/Apfel/Rezepte.html").then( r -> {
//        http.getContent("http://www.spiegel.de").then( r -> {
            System.out.println(r);
        });
        try {
            Thread.sleep(50000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        http.post(
            "https://www.betfair.com/www/sports/fixedodds/readonly/v1/getMarketPrices?xsrftoken=1dea7f90-190f-11e6-9184-a0369f0e665c&_ak=FIhovAzZxtrvphhu&priceHistory=0",
            "{\"currencyCode\":\"GBP\",\"alt\":\"json\",\"locale\":\"en_GB\",\"marketIds\":[\"924.52118643\",\"924.52118648\",\"924.52118653\",\"924.52118658\",\"924.52118663\",\"924.52118668\",\"924.52118673\",\"924.40402572\",\"924.40594949\",\"924.40401998\",\"924.40402167\",\"924.40402697\",\"924.40402133\",\"924.40402201\",\"924.40402846\",\"924.40402595\",\"924.40402628\",\"924.40402666\",\"924.40402773\",\"924.40402249\",\"924.40402232\",\"924.40400946\",\"924.40402051\",\"924.40402099\",\"924.40402209\",\"924.40402635\",\"924.40402731\",\"924.40402863\",\"924.40402663\",\"924.40402812\",\"924.40402798\",\"924.40401939\",\"924.40402180\",\"924.40402012\",\"924.40402144\",\"924.40402067\",\"924.40402106\",\"924.40402596\",\"924.40402764\",\"924.40402830\",\"924.40402880\",\"924.40402700\",\"924.40402735\",\"924.8444186\",\"924.52118678\",\"924.52118683\"]}\n" +
                "Name\n" +
                "football?modules=matchupdates%401038&lastId=1070&ts=1463161488790&alt=json&xsrftoken=1dea7f90-190f-11e6-9184-a0369f0e665c\n" +
                "getMarketPrices?xsrftoken=1dea7f90-190f-11e6-9184-a0369f0e665c&_ak=FIhovAzZxtrvphhu&priceHistory=0\n" +
                "football?modules=matchupdates%401038&lastId=1070&ts=1463161503791&alt=json&xsrftoken=1dea7f90-190f-11e6-9184-a0369f0e665c\n" +
                "getMarketPrices?xsrftoken=1dea7f90-190f-11e6-9184-a0369f0e665c&_ak=FIhovAzZxtrvphhu&priceHistory=0\n" +
                "football?modules=matchupdates%401038&lastId=1070&ts=1463161518790&alt=json&xsrftoken=1dea7f90-190f-11e6-9184-a0369f0e665c\n" +
                "getMarketPrices?xsrftoken=1dea7f90-190f-11e6-9184-a0369f0e665c&_ak=FIhovAzZxtrvphhu&priceHistory=0\n",
            "Content-Type","application/json"
        )
            .then((r, e) -> {
                System.out.println(r + " " + e);
                try {
                    String s = AsyncHttpActor.readContentString(r);
                    System.out.println("result:"+s);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });
        if (1 != 0) {
            return;
        }
    }


}
