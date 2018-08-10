// @flow
import firebase from 'firebase';
import 'firebase/firestore';

const config = require("../config/config.json");
firebase.initializeApp(config);

let db = firebase.firestore();

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
  // Popup signin flow rather than redirect flow.
  signInFlow: 'popup',
  signInSuccessUrl: '/protected',
  // We will display Google and Facebook as auth providers.
  signInOptions: [
    firebase.auth.EmailAuthProvider.PROVIDER_ID,
    firebase.auth.GoogleAuthProvider.PROVIDER_ID,
  ],
};

export default {
  getScheduleSubscriber,
  addClass,

  uiConfig,
};
