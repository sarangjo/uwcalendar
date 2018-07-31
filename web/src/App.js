import React, { Component } from 'react';
import { Provider, BrowserRouter, Switch, Route } from 'react-router-dom';

import Home from './pages/Home.react';

import './App.css';

class App extends Component {
  render() {
    return (
      <BrowserRouter>
        <Switch>
          <Route path="/" component={Home} />
        </Switch>
      </BrowserRouter>
    );
  }
}

export default App;
