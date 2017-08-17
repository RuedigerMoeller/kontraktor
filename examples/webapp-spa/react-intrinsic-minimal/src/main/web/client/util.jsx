import {Component}  from 'react'

export class HCenter extends React.Component {

  render() {
    const style = {
      alignContent: 'center',
      alignItems: 'center',
      boxSizing: 'border-box',
      display: 'flex',
      flexDirection: 'row',
      flexWrap: 'nowrap',
      justifyContent: 'center',
      ...this.props.style
    };
    return (<div style={style}>{this.props.children}</div>)
  }

}

export class Fader extends React.Component {
  constructor(p) {
    super(p);
    this.state={op:0};
  }

  componentDidMount() {
    setTimeout( () => this.setState({op:1}), 100)
  }
  render() {
    const style = {
      opacity: this.state.op,
      transition: "opacity .5s"
    };
    return (<div style={style}>{this.props.children}</div>)
  }
}
