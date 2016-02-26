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
var quarterInfo = null;

// Authorization client
var oauth2Client = null;


//////////////// HELPERS ////////////////

// Standard String format: 2016-01-28T17:00:00-07:00
function dateToString(date) {
	var str = date.toISOString().substring(0, 19);
	return str + TIMEZONE;
}

/////////////////////////////////////////

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
		// Setup our client with the loaded credentials.
		setupApp(res, JSON.parse(content));
	}); 
});

// Setup a client to access the calendar, and continue the workflow.
function setupApp(res, credentials) {
	var clientSecret = credentials.installed.client_secret;
	var clientId = credentials.installed.client_id;
	// 0: default page, 1: localhost
	var redirectUrl = credentials.installed.redirect_uris[1];
	var auth = new googleAuth();

	oauth2Client = new auth.OAuth2(clientId, clientSecret, redirectUrl);

	// Now that the client has been setup, we go on to user auth
	checkIfUserAuthorized(res);
}

/**
 * Check if we have previously stored a token for this user.
 * If we have, jumps right to the entry page.
 * If not, prompts user to login.
 */
function checkIfUserAuthorized(res) {
	fs.readFile(TOKEN_PATH, function(err, token) {
		if (err) {
			var authUrl = oauth2Client.generateAuthUrl({
				access_type: 'offline',
				scope: SCOPES
			});
			renderLogin(res, authUrl);
		} else {
			oauth2Client.credentials = JSON.parse(token);
			// Jump to post-setup
			res.redirect('classes');
		}
	});
}

// If the user has not been authorized, renders the login screen with the authorization URL.
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
			renderFailedAuth(res, false);
			return;
		}
		oauth2Client.credentials = token;
		storeToken(token);
		res.redirect('classes');
	});
});

// Render what needs to be rendered if the user was not actually authorized
function renderFailedAuth(res) {
	var params = { result: "Failed!", loggedout: true };
	res.render('authorize', params );
}

// Saves the user auth token locally
// TODO: save in database?
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

/* GET classes page. */
router.get('/classes', function(req, res) {
	if (oauth2Client == null) {
		res.redirect('/login');
		return;
	}
	var params = {};
	params.error = req.session.error;
	params.message = req.session.message;

	// Adding the quarter details object to the session
	if (quarterInfo == null) {
		fs.readFile(QTR_DETAILS, function(err, qtrDetailsFileContent) {
			if (err) {
				console.log('Error loading quarter info: ' + err);
				return;
			}
			quarterInfo = JSON.parse(qtrDetailsFileContent);
			renderClasses(req, res, params);
		});
	} else {
		renderClasses(req, res, params);
	}
});

// Lists events in the console, and one on the page
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

function renderClasses(req, res, params) {
	params.title = 'Classes';
	res.render('classes', params);
}

/* POST addclass page. Adds new event and redirects to /classes page. */
router.post('/addclass', function(req, res) {
	addClass(req, res, 0);
});

var n = 2;

/**
 * Creates and adds a new class to the calendar.
 * 
 * @param qtrDetailsFileContent 
 *		content from the file
 * @param body
 * 		req.body
 * @param index
 *		index of the class
 * @param doneCallback
 * 		the function to call when the last class has been added
 */
function addClass(req, res, index) {
	// Check to see if all classes have been added
	if (index >= n) {
		console.log("Done adding classes.");
		res.redirect('/classes');
		return;
	}

	// Error-checking
	// hasErrors automatically modifies the res session
	if (hasErrors(req, index)) {
		// TODO: pass original values of input elements back to HTML form
		res.redirect('/classes');
		return;
	}

	// We are error-free!
	var calendar = google.calendar('v3');

	// Insert the class
	calendar.events.insert({
		auth: oauth2Client,
		calendarId: 'primary',
		resource: createClass(req, index)
	}, function(err, event) {
		if (err) {
			console.log('There was an error contacting the Calendar service: ' + err);
			req.session.error = err;
			req.session.message = "";
		} else {
			var message = "Class #" + index + " added!";
			console.log(message);
			req.session.message = message;
		}
		addClass(req, res, index + 1);
	});
}

/**
 * Constructs a class from the request body.
 */
function createClass(req, index) {
	var addedClass = {
		summary: req.body["classname" + index],
		location: req.body["classlocation" + index],
	};

	// RECURRENCE DETAILS
	var qtrDetails = quarterInfo[req.body.classqtr];
	var enddate = (qtrDetails.end).replace(new RegExp("-", 'g'), "");
	var days = parseDays(req.body);
	addedClass.recurrence = ['RRULE:FREQ=WEEKLY;UNTIL=' + enddate + 'T115959Z;WKST=SU;BYDAY=' + days];

	// Expand start/end time to include full date
	start = qtrDetails.start + "T" + req.body["classstart" + index] + ":00" + TIMEZONE;
	end = start.substring(0, 11) + req.body["classend" + index] + start.substring(16);
	addedClass.start = { dateTime: start, timeZone: 'America/Los_Angeles' };
	addedClass.end =  { dateTime: end, timeZone: 'America/Los_Angeles' };

	return addedClass;
}

// index is the index of the class being added, 0-based
function hasErrors(req, index) {
	req.session.error = [];
	
	if (quarterInfo == null) {
		req.session.error.push("Internal error. Quarter info invalid.");
	}

	if (req.body["classname" + index] == "") {
		req.session.error.push("Enter a valid class name.");
	}
	
	if (req.body["classlocation" + index] == "") {
		req.session.error.push("Enter a valid class location.");
	}
	
	var start = req.body["classstart" + index];
	var end = req.body["classend" + index];
	var timesOK = timesValid(start,end);
	if (!timesOK) {
			req.session.error.push("Enter valid start/end times.");
	}	
	
	var days = parseDays(req.body);
	if (days == "") {
		req.session.error.push("Select at least 1 day.");
	}
	
	return (req.session.error.length != 0);
}

// Verifies that start is before end
// Format 09:30, 21:30
function timesValid(start, end) {
	if (start == '' || end == '') return false;
	var starthr = parseInt(start.substring(0,2));
	var endhr = parseInt(end.substring(0,2));
	if (starthr > endhr) return false;
	else if (starthr < endhr) return true;
	else return (parseInt(start.substring(3))) < (parseInt(end.substring(3)));
}

// Parses out the days string from the request body
function parseDays(reqBody) {
	var start = true;
	var days = "";

	if (reqBody.hasOwnProperty("monday")) {
		start = false;
		days += "MO";
	}
	if (reqBody.hasOwnProperty("tuesday")) {
		if (!start) days += ",";
		else start = false;
		days += "TU";
	}
	if (reqBody.hasOwnProperty("wednesday")) {
		if (!start)	days += ",";
		else start = false;
		days += "WE";
	}
	if (reqBody.hasOwnProperty("thursday")) {
		if (!start) days += ",";
		else start = false;
		days += "TH";
	}
	if (reqBody.hasOwnProperty("friday")) {
		if (!start) days += ",";
		else start = false;
		days += "FR";
	}

	return days;
}

/* GET logout page. Deletes OAuth2 token. */
router.get('/logout', function(req, res) {
	oauth2Client = null;

	// delete authorization token
	fs.unlinkSync(TOKEN_PATH);

	console.log("Auth token deleted.");

	res.redirect('/');
});

module.exports = router;
