import React, { Component } from 'react';
import { BrowserRouter, Switch, Route, Link } from 'react-router-dom';
import firebase from 'firebase';

import Login from './components/Login.react';
import Home from './components/Home.react';

import { component } from './util';

import './App.css';

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      user: null,
      loading: true,
    };
  }

  componentDidMount() {
    // FIXME:  Assumes this always fires once initially?
    this.unAuth = firebase.auth().onAuthStateChanged((user) => {
      this.setState({
        user: user,
        loading: false,
      });
    });
  }

  componentWillUnmount() {
    if (this.unAuth) this.unAuth();
  }

  render() {
    const { user, loading } = this.state;
    let content;

    if (loading) {
      content = (
        <div>Loading...</div>
      );
    } else if (!user) {
      content = (
        <div>
          <h1>Not signed in</h1>
          <Login/>
        </div>
      );
    } else if (user) {
      // NOTE make sure the routes are in decreasing order of specificity, with /
      // being last
      content = (
        <div>
          <h1>Welcome to UW Calendar, {user.displayName}!</h1>
          <Switch>
            <Route path="/" render={component(Home, user)}/>
          </Switch>
        </div>
      );
    }

    return (
        <div className="container">
          <BrowserRouter>
            {content}
          </BrowserRouter>
        </div>
    )
  }
}

export default App;
