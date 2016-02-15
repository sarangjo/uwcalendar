var express = require('express');
var router = express.Router();

var fs = require('fs');
var google = require('googleapis');
var googleAuth = require('google-auth-library');

var SCOPES = ['https://www.googleapis.com/auth/calendar'];
var TOKEN_DIR = 'credentials/';
var TOKEN_PATH = TOKEN_DIR + 'uwgooglecal-cred.json';

// TODO: Make this DST-dependent
var TIMEZONE = "-08:00";

var QTR_DETAILS = 'details/qtr-details.json';

// Authorization client
var oauth2Client = null;

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'UW Google Cal', loggedout: true });
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

// Authorize a client to access the calendar
// authCallback is where to come after the user has been authenticated
function authorize(res , credentials, authCallback) {
  var clientSecret = credentials.installed.client_secret;
  var clientId = credentials.installed.client_id;
  // 0: default page, 1: localhost
  var redirectUrl = credentials.installed.redirect_uris[1];
  var auth = new googleAuth();

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
  res.render('login', { title: 'Login', auth: authUrl, loggedout: true });
}

/* GET authorized page. This is after retrieving the OAuth2 token. */
router.get('/authorized', function(req, res) {
  // The auth token is in the "code" GET param
  var token = req.query.code;

  oauth2Client.getToken(token, function(err, token) {
	if (err) {
	  console.log("Access token error", err);
	  renderAuthorized(res, false);
	}

	oauth2Client.credentials = token;
	storeToken(token);
	renderAuthorized(res, true);
  });
});

// Render what needs to be rendered once the user has been authorized
function renderAuthorized(res, isAuthorized) {
  if (isAuthorized)
	res.redirect('classes');
  else {
	var params = { result: "Failed!", loggedout: true };
	res.render('authorize', params );
  }
}

// Saves the token locally
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

// Standard String format: 2016-01-28T17:00:00-07:00
function dateToString(date) {
  var str = date.toISOString().substring(0, 19);
  return str + TIMEZONE;
}

/* GET classes page. */
router.get('/classes', function(req, res) {
  if (oauth2Client == null) {
	res.redirect('/login');
	return;
  }

  //listCals(req, res, oauth2Client, renderEvents);
  listEvents(req, res, oauth2Client, renderEvents);
});

function listEvents(req, res, auth, callback) {
  var calendar = google.calendar('v3');
  var minDate = dateToString(new Date(2016, 1, 17, 0, 0, 0));
  var maxDate = dateToString(new Date(2016, 1, 17, 11, 59, 59));

  // List the next 10 events in the primary calendar.
  calendar.events.list({
	auth: auth,
	calendarId: 'primary',
	timeMin: minDate,
	timeMax: maxDate,
	maxResults: 10,
	singleEvents: true,
	orderBy: 'startTime'
  }, function(err, response) {
	if (err) {
	  console.log('The API returned an error: ' + err);
	  res.redirect('/');
	}
	var events = response.items;
	var result = {};
	if (events.length == 0) {
	  console.log('No upcoming events found.');
	} else {
	  console.log('Upcoming 10 events:');
	  for (var i = 0; i < events.length; i++) {
		var event = events[i];
		var start = event.start.dateTime || event.start.date;
		console.log('%s - %s', start, event.summary);
	  }
	  var event = events[0];
	  result.eventsummary = event.summary;
	  result.eventdate = (event.start.dateTime || event.start.date);
	}
	callback(req, res, result);
  });
}

function renderEvents(req, res, params) {
  params.title = 'Events';
  res.render('classes', params);
}

/* POST addevent page. Adds new event and redirects to /classes page. */
router.post('/addevent', function(req, res) {
	var name = req.body.classname;
	var loc = req.body.classlocation;
	var starthr = parseInt(req.body.classstart);
	var endhr = starthr+1;
	// 0-padding
	starthr = ((starthr < 10) ? ("0" + starthr) : starthr);
	
	var qtr = req.body.classqtr;
	
	// Recurrence info
	fs.readFile(QTR_DETAILS, function(err, content) {
		if (err) {
		  console.log('Error loading quarter info: ' + err);
		  return;
		}

		var event = getEvent(name, loc, starthr, endhr, JSON.parse(content)[qtr]);

		var calendar = google.calendar('v3');

		calendar.events.insert({
			auth: oauth2Client,
			calendarId: 'primary',
			resource: event
		}, function(err, event) {
			if (err) {
				console.log('There was an error contacting the Calendar service: ' + err);
			} else {
				console.log("Event logged! Event info:");
				console.log(event);
			}
			res.redirect('classes');
		});
  	});
});

/** 
 * Creates the actual event resource object and returns it.
 */
function getEvent(name, loc, starthr, endhr, qtrDetails) {
	// Recurrence details
	var startdate = qtrDetails.start;  //2016-xx-xx

	var start = startdate + "T" + starthr + ":30:00" + TIMEZONE;
	var end = start.substring(0, 11);
	end += endhr;
	end += ":20";
	end += start.substring(16);

	var enddate = (qtrDetails.end).replace(new RegExp("-", 'g'), "");

	var recurrenceInfo = 'RRULE:FREQ=WEEKLY;UNTIL=' + enddate + 'T115959Z;WKST=SU;BYDAY=MO,WE,FR'

	return {
		summary: name,
		start: {
			dateTime: start,
			timeZone: 'America/Los_Angeles',
		}, end: {
			dateTime: end,
			timeZone: 'America/Los_Angeles',
		},
		location: loc,
		// Repeat MWF till end of quarter
		recurrence: [
       		recurrenceInfo
		]
	};
}

/* GET logout page. Deletes OAuth2 token. */
router.get('/logout', function(req, res) {
	oauth2Client = null;

	// delete authorization token
	fs.unlinkSync(TOKEN_PATH);

	console.log("auth token deleted");

	res.redirect('/');
});

module.exports = router;
