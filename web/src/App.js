import React, { Component } from 'react';
import { Provider } from 'react-redux';
import { BrowserRouter, Switch, Route } from 'react-router-dom';
import firebase from 'firebase';

import Home from './pages/Home.react';

import './App.css';

class App extends Component {
  render() {
    return (
      <BrowserRouter>
        <Switch>
          <Route path="/test" component={Home} />
        </Switch>
      </BrowserRouter>
    );
  }
}

export default App;
