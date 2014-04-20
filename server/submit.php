<?php

include("db.php");

if(isset($_REQUEST['bsStart']) && isset($_REQUEST['timeStart']) && isset($_REQUEST['bsEnd']) && isset($_REQUEST['timeEnd'])) {
	if($stmt = $conn->prepare("INSERT INTO tripsegment VALUES(?, ?, ?, ?)")) {
		$stmt->bind_param('isis', $_REQUEST['bsStart'], $_REQUEST['timeStart'], $_REQUEST['bsEnd'], $_REQUEST['timeEnd']);
		$stmt->execute();
		echo $conn->error;
	}
} else if(isset($_REQUEST['bsid']) && isset($_REQUEST['timeStart']) && isset($_REQUEST['timeWait'])) {
	if($stmt = $conn->prepare("INSERT INTO busstopwait VALUES(?, ?, ?)")) {
		$stmt->bind_param('iss', $_REQUEST['bsid'], $_REQUEST['timeStart'], $_REQUEST['timeWait']);
		$stmt->execute();
		echo $conn->error;
	}
}

?>