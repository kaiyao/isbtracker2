<?php

include("db.php");

if(isset($_REQUEST['bs'])) {
	#to be implemented - get time for individual busstop
} else {
	if ($stmt = $conn->prepare('SELECT b.busstop, DAYNAME(b.timestart) AS day, TIME(b.timestart) AS time, AVG(b.waittime) AS wait 
								FROM busstopwait b 
								GROUP BY b.busstop, UNIX_TIMESTAMP(time(b.timestart)) DIV 900, DAYNAME(b.timestart)')) {
		$stmt->execute();
		echo $conn->error;
		$result = $stmt->get_result();
		$resStr = "";

		while($row = $result->fetch_assoc()) {
			$resStr = $resStr.$row['busstop'].','.$row['day'].','.$row['time'].','.$row['wait'].",\r\n";
		}

		header("Content-type: text/csv");
		header('Content-disposition: attachment;filename=Timings.csv');
		echo $resStr;
	}
}

?>