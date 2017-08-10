

import { VSpacer, EmptyLine, HCenter, Caption, Table, Tr, Td } from './layout.jsx';
import React from 'react';
import {AppActions, Store as AppStore} from './store.jsx';
import {Btn} from "./login.jsx";

function Unverified() {
  return (
    React.createElement(
      'div',
      {
        style: { width: 16, background: "tomato", color: 'white', padding: 4, border: '1px solid #eee'}
      },
      React.createElement(
        HCenter,
        null,
        '!'
      )
    )
  )
}

function Verified() {
  return (
    React.createElement(
      'div',
      {
        style: { width: 16, color: "white", background: 'green', padding: 4, border: '1px solid #eee'}
      },
      React.createElement(
        HCenter,
        null,
        'Ok'))
  )
}

let idCount = 0;
export class UserTable extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      records: []
    };
    this.idCount = idCount++;
    console.log("created "+this.idCount)
  }

  componentDidMount() {
    const decoder = new DecodingHelper();
    AppActions.queryUsers( (r,e) => {
      console.log("reply ",this.idCount);
      if ( r ) {
        let jsmap = decoder.jsmap(r.map);
        jsmap["key"] = r.key;
        const copy = this.state.records.slice();
        copy.push(jsmap);
        this.setState({ records: copy });
      } else {
        console.log("query finished")
      }
    });
  }

  onDel(key,index) {
    AppActions.deleteUser(key);
    const copy = this.state.records.slice();
    copy.splice(index,1);
    this.setState({ records: copy });
  }

  render() {
    let count = 0;
    const grey = { background: "#eee" };
    const none = {};
    return (
      React.createElement(
        Table,
        {
          bg:'#fff',
          style: { minWidth: "50%"}
        },
        React.createElement(
          Tr,
          null,
          React.createElement(
            Caption,
            null,
            'User')),
        React.createElement(
          EmptyLine,
          null
        ),
        this.state.records.map( (rec,index) =>
          React.createElement(
            Tr,
            {
              key: rec.key
            },
            React.createElement(
              Td,
              {
                style:  ((++count%2) == 1) ? grey : none
              },
              rec.key ),
            React.createElement(
              Td,
              {
                style:  ((count%2) == 1) ? grey : none
              },
              rec.pwd ),
            React.createElement(
              Td,
              {
                style:  ((count%2) == 1) ? grey : none
              },
              rec.verified ? React.createElement(
                Verified,
                null
              ): React.createElement(
                Unverified,
                null
              ) ),
            React.createElement(
              Td,
              {
                style:  ((count%2) == 1) ? grey : none
              },
              rec.text ),
            React.createElement(
              Td,
              {
                style:  ((count%2) == 1) ? grey : none
              },
              rec.count ),
            React.createElement(
              Td,
              {
                style:  ((count%2) == 1) ? grey : none
              },
              React.createElement(
                Btn,
                {
                  name: rec.key ,
                  onClick: () => this.onDel(rec.key,index)
                },
                'x')),
          )) )
    )
  }

}
