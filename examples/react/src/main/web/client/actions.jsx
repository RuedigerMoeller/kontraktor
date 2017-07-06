import AppDispatcher from './dispatcher.jsx';

class AppActions {

  login(user,password) {
    AppDispatcher.dispatch({
      actionType: 'TRY_LOGIN',
      value: { user: user, password: password }
    });
  }

  connected() {
    AppDispatcher.dispatch({
      actionType: 'CONNECTED',
      value: true
    });
  }

}

export default new AppActions()