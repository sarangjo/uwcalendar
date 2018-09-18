// @flow
import firebase from 'firebase';
import 'firebase/firestore';

import goog from './goog';

const config = require("../config/config.json");
firebase.initializeApp(config);

let db = firebase.firestore();
db.settings({ timestampsInSnapshots: true });

// Helper to get to the `classes` collection
function getClassesCollection(uid, quarter) {
  return db.collection("schedules").doc(uid)
  .collection("quarters").doc(quarter)
  .collection("classes");
}

// Returns unsub
// callback is (data blob) => ()
function getScheduleSubscriber(uid, quarter, callback) {
  return getClassesCollection(uid, quarter)
  .onSnapshot((s) => {
    // Sanitizing an empty schedule
    callback(s.docs.map(d => { return { ...d.data(), id: d.id } }));
  });
}

// Returns a Promise with the created document
function addClass(uid, quarter, o) {
  // TODO sanitize o
  let {id, ...oWithoutId} = o;
  return getClassesCollection(uid, quarter).add(oWithoutId);
}

// Returns a Promise with the updated document
function updateClass(uid, quarter, classId, o) {
  // TODO sanitize o
  let {id, ...oWithoutId} = o;
  return getClassesCollection(uid, quarter).doc(classId)
  .set(oWithoutId);
}

// Returns a Promise that resolves with an array of success values
function updateGoogleEventIds(uid, quarter, schedule, googleEvents) {
  let promises = schedule.map((c, i) => {
    return getClassesCollection(uid, quarter).doc(c.id)
    .update({
      googleEventId: googleEvents[i].id
    });
  });
  return Promise.all(promises);
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
  updateClass,
  updateGoogleEventIds,

  uiConfig,
};
