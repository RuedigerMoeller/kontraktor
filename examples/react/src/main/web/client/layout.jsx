import React from 'react';

export class Table extends React.Component {

  render() {
    const style = { display: 'table', background: '#eee', padding: '16px', border: 'solid #ddd 1px' };
    return (
      <div style={style}>
        {this.props.children}
      </div>
    )
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
    const style = { display: 'table-cell', padding: '4px' };
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