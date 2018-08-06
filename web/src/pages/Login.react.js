// @flow
// Import FirebaseAuth and firebase.
import React from 'react';
import StyledFirebaseAuth from 'react-firebaseui/StyledFirebaseAuth';
import firebase from 'firebase';

import { uiConfig } from '../helpers/auth';

class Login extends React.Component {
  render() {
      return (
        <div>
          <h1>My App</h1>
          <p>{firebase.auth().currentUser ? firebase.auth().currentUser.displayName : "Sign in needed"}</p>
          <p>Please sign-in:</p>
          <StyledFirebaseAuth uiConfig={uiConfig} firebaseAuth={firebase.auth()}/>
        </div>
      );
  }
}

export default Login;
