package icebreakrest;

import java.io.IOException;

/**
 * Created by ruedi on 12.08.2014.
 */
public class TestIce {

    public static void main(String[] args) throws IOException {

        // Declare the IceBreak HTTP REST server class
        IceBreakRestServer rest;


        try {

            // Instantiate it once
            rest  = new IceBreakRestServer();
            rest.debug = true;


            while (true) {

                // Now wait for any HTTP request
                // the "config.properties" file contains the port we are listening on
                rest.getHttpRequest();

                // If we reach this point, we have received a request
                // now we can pull out the parameters from the query-string
                // if not found we return the default "N/A"
//                String name = rest.getQuery("name", "N/A");
                rest.write("resource: " + rest.request);
                rest.write("\n");
                rest.write("method: " + rest.method);
                rest.write("\n");
                rest.write("resource: " + rest.resource);
                rest.write("\n");
                rest.write("queryStr: " + rest.queryStr);
                rest.write("\n");
                rest.write("httpVer: " + rest.httpVer);
                rest.write("\n");
                rest.write("header  : " + rest.header   );
                rest.write("\n");
                rest.write("parms : " + rest.parms);
                rest.write("\n");

            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}