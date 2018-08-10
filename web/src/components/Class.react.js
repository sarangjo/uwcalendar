// @flow
import React from 'react';

class Class extends React.Component {
  render() {
    return (
      <div>
        Class: {JSON.stringify(this.props.data)}
      </div>
    );
  }
}

export default Class;
