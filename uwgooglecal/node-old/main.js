var http = require("http");

// Create a server to be used in a web browser
var port = 3003;

/*http.createServer(function(request, response) {
	response.writeHead(200,{'Content-Type': 'text/plain'});

	response.end('Hello World!\n');
}).listen(port);

console.log('Server running on port ' + port + '.');*/

// File systems
var fs = require("fs");

fs.readFile('lel.txt', function(err, data) {
	if (err) return console.error(err);
	console.log(data.toString());
});

console.log('Done reading file');