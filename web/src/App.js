import React, { Component } from 'react';
import { Provider } from 'react-redux';
import { BrowserRouter, Switch, Route } from 'react-router-dom';
import firebase from 'firebase';

import Container from './pages/Container.react';
import Login from './pages/Login.react';
import Home from './pages/Home.react';

import './App.css';

class App extends Component {
  render() {
    // NOTE make sure the routes are in decreasing order of specificity, with /
    // being last
    return (
      <Container>
        <BrowserRouter>
          <Switch>
            <Route path="/login" component={Login} />
            <Route path="/" component={Home} />
          </Switch>
        </BrowserRouter>
      </Container>
    );
  }
}

export default App;
