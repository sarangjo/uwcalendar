<?php
require 'vendor/autoload.php';

define('APPLICATION_NAME', 'Google Calendar API PHP Quickstart');
define('CLIENT_SECRET_PATH', 'client_secret.json');
define('SCOPES', implode(' ', array(
	Google_Service_Calendar::CALENDAR_READONLY)
));

/**
 * Returns an authorized API client.
 * @return Google_Client the authorized client object
 */
function getClient() {
	$client = new Google_Client();
	$client->setApplicationName(APPLICATION_NAME);
	$client->setScopes(SCOPES);
	$client->setAuthConfigFile(CLIENT_SECRET_PATH);
	$client->setAccessType('offline');

	return $client;
}

?>

<html>
<head><title>UW Google Calendar</title></head>
<body>
	<p>Welcome to UW Google Calendar!</p>
	<p>
		<?php $authUrl = getClient()->createAuthUrl(); ?>
		<a href="<?php echo $authUrl; ?>"><?php echo $authUrl; ?></a>
	</p>
</body>
</html>
