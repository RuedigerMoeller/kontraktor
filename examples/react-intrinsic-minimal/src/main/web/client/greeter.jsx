import {Component} from 'react';
import {global} from "./global";
import {Fader} from "./util";

class Greeter extends Component {

  constructor(props) {
    super(props);
    this.state = {
      greeting: '...'
    };
  }

  componentDidMount() {
    global.session.greet('World')
      .then( (res,err) => {
        console.log("receive greeting ",res,err);
        this.setState({greeting: err ? err : res });
      });
  }

  render() {
    /*enforce component creation to trigger fading*/
    return (
      <Fader>
        { this.state.greeting == '...' ?
          <div>{this.state.greeting}</div>
          :
          <div>{this.state.greeting}</div>
      }
      </Fader>
    );
  }

}