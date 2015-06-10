
function ViewModel() {

    var self = this;
    var errCB = function( err ) { console.error(err); };

    self.server = null;
    self.debug ="POK";
    self.user = ko.observable("");
    self.pwd = ko.observable("");
    self.text= ko.observable("Welcome to Kontraktor/4k 's sample Single Page Application (SPA).");
    self.loggedin = ko.observable(false);


    self.login = function() {

        if ( self.user().trim().length == 0 ) {
            self.text( "<font color='red'>Please enter at least a user name</font>" );
            return;
        }

        //    jsk.connect("ws://localhost:8080/ws","WS",errCB) // use this for websockets
        var location = window.location+"/api";
        location = location.replace("//api","/api");
        jsk.connect( location,"HTLP",errCB) // use this for long poll
            .then( function( app, error ) {
                if ( ! app ) {
                    self.text("<font color='red'>connection failure</font>");
                    console.error(error);
                }
                server = app;

                server.ask("login", self.user(), self.pwd() )
                    .then( function(mySession,err) {
                        if ( err ) {
                            self.text("<font color='red'>login failure</font>");
                            console.log(err);
                        }
                        else {
                            self.loggedin(true);
                            // login done
                            console.log("login done");
                        }
                    })
            });
    };
}

var model = new ViewModel();
ko.punches.enableAll();
ko.applyBindings(model);