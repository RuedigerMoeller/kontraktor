var app = angular.module("fst-test", ['ui.bootstrap'])

app.controller('TestCtrl', function ($scope) {

    $scope.host = 'localhost';
    $scope.port = '8887';
    $scope.websocketDir = "websocket";
    $scope.socketConnected = false;

    $scope.doConnect = function () {
        var ws = new WebSocket("ws://".concat($scope.host).concat(":").concat($scope.port).concat("/").concat($scope.websocketDir));
        ws.onopen = function () {
            console.log("open");
            $scope.$apply(function () {
                $scope.socketConnected = true;
            });
        };
        ws.onerror = function () {
            console.log("error");
            $scope.$apply(function () {
                $scope.socketConnected = false;
            });
        };
        ws.onclose = function () {
            console.log("closed");
            $scope.$apply(function () {
                $scope.socketConnected = false;
            });
        };
        ws.onmessage = function (message) {
            var fr = new FileReader();
            if ( typeof message.data == 'string' ) {
                $scope.$apply(function () {
                    $scope.resptext = message.data;
                });
            } else {
                fr.onloadend = function (event) {
                    //var msg = MinBin.decode(event.target.result);
                    //var strMsg = MinBin.prettyPrint(msg);
                    $scope.$apply(function () {
                        // handle message
                    });
                };
                // error handling is missing
                fr.readAsArrayBuffer(message.data);
            }
        };
        $scope.ws = ws;
    };

    $scope.doSend = function() {
        $scope.ws.send($scope.text);
    }
});
