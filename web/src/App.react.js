import React, { Component } from 'react';
import { BrowserRouter, Switch, Route } from 'react-router-dom';
import { Button } from '@material-ui/core';
import firebase from 'firebase';

import './App.css';

import Login from './components/Login.react';
import Home from './components/Home.react';
import { component } from './util';
import goog from './services/goog';

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      user: null,
      loading: true,
    };
  }

  componentDidMount() {
    this.unAuth = firebase.auth().onAuthStateChanged((user) => {
      this.setState({
        user: user, loading: false,
      });
    });
  }

  componentWillUnmount() {
    if (this.unAuth) this.unAuth();
  }

  handleLogout() {
    firebase.auth().signOut();
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
          <Button onClick={this.handleLogout.bind(this)}>Log out</Button>
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
