<?php
$fp = fopen("id.txt", "a");
fwrite($fp, $_POST['regId']);
fwrite($fp, "\n");
fclose($fp);
?>

