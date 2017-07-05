import React from 'react';

export class Table extends React.Component {

  render() {
    const style = { display: 'table' };
    return <div style={style}>
      {this.props.children}
    </div>
  }

}

export class Tr extends React.Component {

  render() {
    const style = { display: 'table-row' };
    return <div style={style}>
      {this.props.children}
    </div>
  }

}

export class Td extends React.Component {

  render() {
    const style = { display: 'table-cell' };
    return <div style={style}>
      {this.props.children}
    </div>
  }

}