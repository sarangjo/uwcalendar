// @flow
import firebase from 'firebase';
import 'firebase/firestore';
import fs from 'fs';

import goog from './goog';

const config = require("../config/config.json");
firebase.initializeApp(config);

let db = firebase.firestore();
db.settings({ timestampsInSnapshots: true });

// Returns unsub
// callback is (data blob) => ()
function getScheduleSubscriber(uid, quarter, callback) {
  return db.collection("schedules").doc(uid)
  .collection("quarters").doc(quarter)
  .collection("classes")
  .onSnapshot((s) => {
    // Sanitizing an empty schedule
    callback(s.docs.map(d => d.data()));
  });
}

// Returns a Promise with the created document
function addClass(uid, quarter, o) {
  return db.collection("schedules").doc(uid)
  .collection("quarters").doc(quarter)
  .collection("classes")
  .add(o);
}

const uiConfig = {
  callbacks: {
    signInSuccessWithAuthResult: function(authResult, redirectUrl) {
      // User successfully signed in.
      // Return type determines whether we continue the redirect automatically
      // or whether we leave that to developer to handle.
      console.log(authResult);
      localStorage.setItem('idToken', authResult.credential.idToken);
      localStorage.setItem('accessToken', authResult.credential.accessToken);

      return true;
    },
  },
  // Popup signin flow rather than redirect flow.
  signInFlow: 'popup',
  signInSuccessUrl: '/',
  // We will display Google and Facebook as auth providers.
  signInOptions: [
    {
      provider: firebase.auth.GoogleAuthProvider.PROVIDER_ID,
      scopes: goog.SCOPES,
    }
  ],
};

export default {
  getScheduleSubscriber,
  addClass,

  uiConfig,
};
