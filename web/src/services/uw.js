// @flow
import moment from 'moment';

const URL_FORMAT = "https://www.washington.edu/students/reg/%02d%02dcal.html";
const url = "https://www.washington.edu/students/reg/1819cal.html";

// From https://www.html5rocks.com/en/tutorials/cors/
function createCORSRequest(method, url) {
    var xhr = new XMLHttpRequest();
    if ("withCredentials" in xhr) {
        // Check if the XMLHttpRequest object has a "withCredentials" property.
        // "withCredentials" only exists on XMLHTTPRequest2 objects.
        xhr.open(method, url, true);
    } else if (typeof XDomainRequest !== "undefined") {
        // Otherwise, check if XDomainRequest.
        // XDomainRequest only exists in IE, and is IE's way of making CORS requests.
        xhr = new XDomainRequest();
        xhr.open(method, url);
    } else {
        // Otherwise, CORS is not supported by the browser.
        xhr = null;
    }
    return xhr;
}

function temp() {
    // Visit the URL, sprintf'ing the year as needed
    var xhr = createCORSRequest('GET', url);
    if (!xhr) {
        throw new Error('CORS not supported');
    }
    xhr.onload = function() {
        console.log(xhr.responseText);
    };
    xhr.onerror = function(err) {
        console.log('Error');
        console.log(err);
    };
    xhr.send();
    // fetch(url)
    // .then(function(response) {
    //     console.log(response);
    // });
    // Parse out the element needed
    // the <table> element after h2 containing "Academic Calendar Summary"
}

const DATE_FORMAT = "YYYY-MM-DD";

// q of the form au18
// Returns array of 2 moment objects
function getQuarterDetails(q) {
  // TODO get
  return {
    start: moment("2018-09-26", DATE_FORMAT),
    end: moment("2018-12-07", DATE_FORMAT),
  };
}

export default {
    getQuarterDetails,
    DATE_FORMAT,
};
