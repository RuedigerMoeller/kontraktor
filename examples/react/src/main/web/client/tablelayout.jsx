import React from 'react';

export class Table extends React.Component {

  render() {
    const style = { display: 'table', background: '#eee', padding: '16px', border: 'solid #ddd 1px' };
    return <div style={style}>
      {this.props.children}
    </div>
  }

}

export class Tr extends React.Component {

  constructor(props) {
    super(props);
  }

  render() {
    const style = { display: 'table-row' };
    return <div style={style}>
      { this.props.children }
    </div>
  }

}

export class Td extends React.Component {

  render() {
    const style = { display: 'table-cell', padding: '4px' };
    return <div style={style}>
      {this.props.children}
    </div>
  }

}