import React from 'react';

export class Table extends React.Component {

  render() {
    const style = {
      display: 'table',
      background: this.props.bg ? this.props.bg:'#eee',
      padding: '16px',
      border: 'solid #ddd 1px', ...this.props.style
    };
    return (
      <div style={style}>
        {this.props.children}
      </div>
    )
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

export class Icon extends React.Component {
  render() {
    const st = "fa "+this.props.icon;
    const style = {
      color:'#aaa',
      fontSize: this.props.small ? 14: 20
    };
    return(<i className={st} style={{...style,...this.props.style}} aria-hidden="true"></i>)
  }
}

export class Tr extends React.Component {

  constructor(props) {
    super(props);
  }

  render() {
    const style = { display: 'table-row' };
    return (
      <div style={style}>
        { this.props.children }
      </div>
    )
  }

}

export class Td extends React.Component {

  render() {
    const pst = this.props.style;
    const style = { display: 'table-cell', padding: '4px', verticalAlign: "middle", ...pst };
    return (
      <div style={style}>
        {this.props.children}
      </div>
    )
  }

}

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

export class Caption extends React.Component {

  render() {
    const style = {
      display: 'table-caption',
      paddingBottom: 8,
      fontWeight: 'bold',
      borderBottom: 'solid 1px #000'
    };
    return (<div style={style}>{this.props.children}</div>)
  }

}

export class EmptyLine extends React.Component {

  render() {
    return (
      <Tr><Td>&nbsp;</Td></Tr>
    )
  }
}

export class VSpacer extends React.Component {

  render() {
    let siz = this.props.size;
    if ( ! siz ) {
      siz = 100
    }
    const style = {
      height: siz
    };
    return (
      <div style={style}/>
    )
  }
}

export class Disabler extends React.Component {

  constructor(props) {
    super(props);
  }
  render() {
    const style = this.props.enabled ? {} : { color:'rgba(0,0,0,.6)', pointerEvents: 'none'};
    return (
      <span style={style}>{this.props.children}</span>
    )
  }

}