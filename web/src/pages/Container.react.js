// @flow
import React from 'react';
import firebase from 'firebase';

import '../styles/Container.css';

class Container extends React.Component {
  constructor(props) {
    super(props);
    this.state = { user: null };
  }

  componentDidMount() {
    this.unAuth = firebase.auth().onAuthStateChanged((user) => {
      this.setState({user: user});
    });
  }

  componentWillUnmount() {
    if (this.unAuth) this.unAuth();
  }

  render() {
    if (!this.state.user) return "Not signed in";

    return (
      <div className="container">
        <h1>Welcome to UW Calendar, {this.state.user.displayName}!</h1>
        {this.props.children}
      </div>
    );
  }
}

export default Container;
