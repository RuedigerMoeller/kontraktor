package org.nustaq.kontraktor.services.web;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.templateapp.WebServer;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.MapRecord;

import java.util.UUID;

/**
 * Created by ruedi on 29.05.17.
 *
 * should be implemented by App class
 */
public interface IRegistration extends IDataConnected {

    default IPromise register(String userName, String useremail, String password, String promoid) {
        RealLiveTable userTable= getTable(getUserTableName());

        // ensure lower case
        final String email = useremail.toLowerCase().trim();

        IPromise res = new Promise();

        // get Session ID
        RemoteConnection connection = Actor.connection.get();
        String sessionId = connection != null ? connection.getSocketRef().getConnectionIdentifier() : null;

        if( userName == null || userName.length() < 4 || userName.length() > 40 ||
            useremail == null || useremail.length() < 3 || useremail.length() > 128 ||
            password == null || password.length() < 6 || password.length() > 40 ||
            (promoid != null && promoid.length() > 2048) )
        {
            return Actors.reject("Invalid username, password or mail");
        }

        userTable.get(email).then((result, error) -> {
            if( result == null )
            {
                MapRecord user = MapRecord.New(email);
                String confId = UUID.randomUUID().toString();
                user
                    .put("email",email)
                    .put("userName",userName)
                    .put("creation",System.currentTimeMillis())
                    .put("pwd",password)
                    .put("promoId",promoid)
                    .put("confirmationId", confId)
                    .put("verified", 0);

                userTable.addRecord(user);
                getTable(getConfirmationTableName()).put(confId, "uid", email);
                sendConfirmationMail("uid",email,confId);
                res.complete("done",  null);
            } else {
                res.complete(null, "" + email + " already registered.");
            }
        });

        return res;
    }

    default void handleRegistrationConfirmation(String[] tokens, HttpServerExchange exchange) {
        getTable( getConfirmationTableName() ).get(tokens[2]).then( (r,e) -> {
            if ( r != null ) {
                getTable(getUserTableName()).atomic(r.getString("uid"),
                user -> {
                    if ( user != null ) {
                        int verified = ((Record) user).getInt("verified");
                        ((Record) user).put("verified",verified+1);
                        exchange.setResponseCode(StatusCodes.FOUND);
                        exchange.getResponseHeaders().put(Headers.LOCATION, "registered.html");
                        exchange.endExchange();
                    } else {
                        Log.Error(WebServer.class,"cannot resolve confirmation id "+tokens[2]);
                        exchange.setResponseCode(404);
                        exchange.endExchange();
                    }
                    return null;
                });
            }
        });
    }

    void sendConfirmationMail(String wapp, String email, String confId);

}
