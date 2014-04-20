<?php 

$dbuname = "root";
$dbpasswd = "";
$conn = mysqli_connect("localhost", $dbuname, $dbpasswd, "cs4222");
if (mysqli_connect_errno($conn)) {
    echo "Failed to connect to MySQL: " . mysqli_connect_error();
}
?>