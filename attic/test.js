(new function () {
  let React = null;
  let Component = null;
  let ReactDOM = null;
  let HCenter = null;
  let Fader = null;
  let Greeter = null;
  let global = null;
  let MySemanticPlay = null;
  let Form = null;
  let Modal = null;
  let KClientListener = null;
  const _initmods = () => {
    React = _kresolve('react/index');
    Component = _kresolve('react/index', 'Component');
    ReactDOM = _kresolve('react-dom/index');
    HCenter = _kresolve('subtest/util', 'HCenter');
    Fader = _kresolve('subtest/util', 'Fader');
    Greeter = _kresolve('subtest/greeter', 'Greeter');
    global = _kresolve('global');
    MySemanticPlay = _kresolve('semantic/semanticplay', 'SemanticPlay');
    Form = _kresolve('semantic-ui-react/dist/commonjs/index', 'Form');
    Modal = _kresolve('semantic-ui-react/dist/commonjs/index', 'Modal');
    KClientListener = _kresolve('kontraktor-client/kontraktor-client', 'KClientListener');
  };
  kaddinit(_initmods);

  class App extends Component {

    constructor(props) {
      super(props);
      this.state = {
        user: '',
        loginEnabled: false,
        loggedIn: false,
        error: null,
        expired: false
      };
      const self = this;
      global.kclient.listener = new class extends KClientListener {
        onInvalidResponse(resp) {
          // session timeout (=401 or 'unknown actor')
          self.setState({expired: true});
        }
      };
    }

    reload() {
      document.location.href = "/";
    }

    handleUChange(ev) {
      this.setState({user: ev.target.value}, () => this.validate());
    }

    validate() {
      this.setState({
        loginEnabled: this.state.user.trim().length > 0
      });
    }

    login() {
      global.kclient
        .connect("/api")
        .then((server, err) => {
          if (err)
            this.setState({error: "" + err});
          else {
            global.server = server;
            server.login(this.state.user)
              .then((session, err) => {
                if (err)
                  this.setState({error: "" + err});
                else {
                  global.session = session;
                  console.log("logged in");
                  this.setState({loggedIn: true});
                }
              })
          }
        });
    }

    componentWillUpdate(nextProps, nextState) {
      if (!this.state.loggedIn && nextState.loggedIn) {
        console.log("will be logged in ..")
      }
    }

    testCall() {
      return React.createElement(
        'p',
        null,
        'Testcase requires kontraktor-http >= 4.18.2');
    }

    render() {
      const p = {a: 1, b: 2, c: 3};
      return (
        React.createElement(
          'div',
          null,
          React.createElement(
            HCenter,
            {
              '_JS_': sprd({'...0': p})
            }
          ),
          React.createElement(
            Modal,
            {
              'open': this.state.expired,
              'header': 'Session expired',
              'content': 'you need to relogin',
              'actions': [
                {key: 'ok', content: 'Ok', color: 'green'},
              ],
              'onClose': () => this.reload()
            }
          ),
          React.createElement(
            HCenter,
            null,
            React.createElement(
              'div',
              {
                'style': {fontWeight: 'bold', fontSize: 18}
              },
              ' Hello World ! ')),
          React.createElement(
            'br',
            null
          ),
          null,
          this.state.loggedIn && this.testCall(),
          this.state.loggedIn ?
            React.createElement(
              Fader,
              null,
              React.createElement(
                Greeter,
                null
              ))
            : (
              React.createElement(
                Fader,
                null,
                React.createElement(
                  HCenter,
                  null,
                  React.createElement(
                    Form,
                    null,
                    React.createElement(
                      Form.Input,
                      {
                        'label': 'Log In',
                        'placeholder': 'nick name',
                        'onChange': ev => this.handleUChange(ev)
                      }
                    ),
                    React.createElement(
                      HCenter,
                      null,
                      React.createElement(
                        Form.Button,
                        {
                          'disabled': !this.state.loginEnabled,
                          'onClick': ev => this.login(ev)
                        },
                        ' Login ')))),
                React.createElement(
                  'br',
                  null
                ))
            )
          ,
          React.createElement(
            'div',
            null,
            this.state.error ? React.createElement(
              'div',
              null,
              React.createElement(
                'b',
                null,
                'error')) : ""),
          React.createElement(
            'div',
            null,
            React.createElement(
              MySemanticPlay,
              null
            )))
      )
    }


  }

  global.app = React.createElement(
    App,
    null
  );

  ReactDOM.render(global.app, document.getElementById("root"));

  kimports['index'] = {};
  kimports['index'].App = App;
  kimports['index'].__kdefault__ = App;
});