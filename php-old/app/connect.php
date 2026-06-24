<?php
function conn()
{
    $host = "localhost";
    $user = "root";
    $pass = 'SysRut.000';
    $db = "servicioMedico";
    return new MySQLi("$host", "$user", "$pass", "$db");
}
