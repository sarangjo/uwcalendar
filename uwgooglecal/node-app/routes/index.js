var express = require('express');
var router = express.Router();

var fs = require('fs');
var google = require('googleapis');
var googleAuth = require('google-auth-library');

var SCOPES = ['https://www.googleapis.com/auth/calendar.readonly'];
var TOKEN_DIR = 'credentials/';
var TOKEN_PATH = TOKEN_DIR + 'uwgooglecal-cred.json';

// Auth variables
var clientSecret;
var clientId;
var redirectUrl;
var auth = new googleAuth();
var oauth2Client;

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'UW Google Cal' });
});

/* GET login page */
router.get('/login', function(req, res, next) {
  // Load client secrets from a local file.
  fs.readFile('client_secret.json', function(err, content) {
    if (err) {
      console.log('Error loading client secret file: ' + err);
      return;
    }
    // Authorize a client with the loaded credentials, and render
    // the login screen if the user isn't already authorized
    authorize(res, JSON.parse(content), renderLogin);
  }); 
});

// Authorize a client
// authCallback is where to come after the user has been authenticated
function authorize(res , credentials, authCallback) {
  clientSecret = credentials.installed.client_secret;
  clientId = credentials.installed.client_id;
  redirectUrl = credentials.installed.redirect_uris[0];
  
  oauth2Client = new auth.OAuth2(clientId, clientSecret, redirectUrl);

  // Check if we have previously stored a token.
  fs.readFile(TOKEN_PATH, function(err, token) {
    if (err) {
      var authUrl = oauth2Client.generateAuthUrl({
        access_type: 'offline',
        scope: SCOPES
      });
      authCallback(res, authUrl);
    } else {
      oauth2Client.credentials = JSON.parse(token);
      // Jump to post-authorize
      renderAuthorized(res, true);
    }
  });
}

// Renders the login screen with the authorization URL.
function renderLogin(res, authUrl) {
  res.render('login', { title: 'Login', auth: authUrl });
}


/* GET userauthorized page. */
router.get('/authorized', function(req, res) {
  // The auth token is in the "token" GET param
  var token = req.query.token;

  oauth2Client.getToken(token, function(err, token) {
    if (err) {
      console.log("Access token error", err);
      return;
    }

    oauth2Client.credentials = token;
    storeToken(token);
    renderAuthorized(res, true);
  });
});

// Render what needs to be rendered once the user has been authorized
function renderAuthorized(res, isAuthorized) {
  if (isAuthorized)
    res.redirect('events');
  else {
    var params = { result: "Failed!" };
    res.render('authorize', params );
  }
}

// Saves the token
function storeToken(token) {
  try {
    fs.mkdirSync(TOKEN_DIR);
  } catch (err) {
    if (err.code != 'EEXIST') {
      throw err;
    }
  }
  fs.writeFile(TOKEN_PATH, JSON.stringify(token));
  console.log('Token stored to ' + TOKEN_PATH);
}

router.get('/events', function(req, res) {
  listEvents(req, res, oauth2Client, renderEvents);
});

function listEvents(req, res, auth, callback) {
  var calendar = google.calendar('v3');
  var result = {};
  calendar.events.list({
    auth: auth,
    calendarId: 'primary',
    timeMin: (new Date()).toISOString(),
    maxResults: 1,
    singleEvents: true
  }, function(err, response) {
    if (err) {
      console.log('The API returned an error: ' + err);
      return;
    }
    var events = response.items;
    if (events.length == 0) {
      console.log('No upcoming events found.');
    } else {
      /*console.log('Upcoming 10 events:');
      for (var i = 0; i < events.length; i++) {
        var event = events[i];
        var start = event.start.dateTime || event.start.date;
        console.log('%s - %s', start, event.summary);
      }*/
      var event = events[0];
      result.eventsummary = event.summary;
      result.eventdate = (event.start.dateTime || event.start.date);
    }
    callback(req, res, result);
  });
}

function renderEvents(req, res, params) {
  params.title = 'Events';
  res.render('events', params);
}

/* GET hello world page. */
router.get('/helloworld', function(req, res) {
  res.render('helloworld', { title: 'Hello World!' });
});

/* GET Userlist page. */
router.get('/userlist', function(req, res) {
  var db = req.db;
  var collection = db.get('usercollection');
  collection.find({}, {}, function(e, docs) {
    res.render('userlist', {
      "userlist" : docs
    });
  });
});

/* GET New User page. */
router.get('/newuser', function(req, res) {
  res.render('newuser', { title: "New User" });
});

/* POST to Add User service. */
router.post('/adduser', function(req, res) {
  var db = req.db;

  var user = req.body.username;
  var email = req.body.useremail;

  var collection = db.get('usercollection');

  collection.insert({
    "username": user,
    "email": email
  }, function(err, doc) {
    if (err) {
      res.send("Error. Lel.");
    } else {
      res.redirect('userlist');
    }
  });
});

module.exports = router;
