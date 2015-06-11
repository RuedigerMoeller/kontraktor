function hilightSessions() {
  var tim = document.getElementById("numSessions");
  if ( tim ) {
    var bright = 1.0;
    // fadeout bgcolor with each event
    var fun = function () {
      tim.style.backgroundColor = "rgba(255,200,100," + bright + ")";
      bright -= .03;
      if (bright >= 0.0) {
        setTimeout(fun,50);
      }
    };
    fun.apply();
  }
}

function ViewModel() {

  var self = this;

  self.server = null;
  self.debug = "POK";
  self.user = ko.observable("");
  self.pwd = ko.observable("");
  self.text = ko.observable("Welcome to Kontraktor/4k 's sample Single Page Application (SPA).");
  self.loggedin = ko.observable(false);
  self.currentView = ko.observable("main");

  self.message = ko.observable("");
  self.messages = ko.observableArray([]);

  self.numSessions = ko.observable("");

  self.sendMsg = function () {
    var m = self.message();
    if (m.length == 0)
      return;
    if (m.length > 1024) {
      m = m.substr(0, 1024) + " [ ... cut, too long ...]";
    }
    self.session.tell("sendMessage", m);
    self.message("");
  };

  self.onMsgEnter = function (keyEvent) {
    keyEvent.keyCode === 13 && self.sendMsg();
    return true;
  };

  self.login = function () {

    if (self.user().trim().length == 0) {
      self.text("<font color='red'>Please choose a nick !</font>");
      return;
    }

    var errCB = function (err) {
      self.text(err);
    };

    var location = window.location.protocol + "//" + window.location.host + "/api";
    location = location.replace("//api", "/api");
    //    jsk.connect("ws://localhost:8080/ws","WS",errCB) // use this for websockets
    jsk.connect(location, "HTLP", errCB) // use this for long poll
      .then(function (app, error) {
        if (!app) {
          self.text("<font color='red'>connection failure</font>");
          console.error(error);
        }
        self.server = app;

        self.server.ask("login", self.user(), self.pwd())
          .then(function (mySession, err) {
            if (err) {
              self.text("<font color='red'>login failure</font>");
              console.log(err);
            }
            else {
              self.session = mySession;
              self.loggedin(true);
              window.location.hash = "main"; // show main template
              // login done, subscribe with delay to give time for window location to switch views
              setTimeout(function () {
                self.session.tell("subscribeChat", function (res, err) {
                  if ( res.msgFrom )
                    self.messages.splice(0, 0, res);
                  else {
                    self.numSessions(" "+res.numSessions+" ");
                    hilightSessions();
                  }
                });
              }, 1000);
              console.log("login done");
            }
          });

      });
  };
}

var model = new ViewModel();
ko.punches.enableAll();
ko.applyBindings(model);

$(window).on('hashchange', function () {
  console.log("change site:" + window.location.hash.substring(1));
  model.currentView(window.location.hash.substring(1)); // bind url hash to template name
});

// called after a view got displayed
function initView() {
  if (window.location.hash == '#about') {
    $('.tlt').textillate({loop: true, initialDelay: 0, minDisplayTime: 1000});
  }
}

