<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

include "db.php";

// Read JSON input
$data = json_decode(file_get_contents("php://input"), true);

if (!$data) {
    echo json_encode(["status" => "error", "message" => "Invalid JSON"]);
    exit;
}

$name        = $data['name'] ?? '';
$email       = $data['email'] ?? '';
$hospital_id = $data['hospital_id'] ?? '';
$password    = $data['password'] ?? '';
$cpassword   = $data['confirm_password'] ?? '';

// Validate
if ($name=="" || $email=="" || $hospital_id=="" || $password=="" || $cpassword=="") {
    echo json_encode(["status" => "error", "message" => "All fields are required"]);
    exit;
}

if ($password !== $cpassword) {
    echo json_encode(["status" => "error", "message" => "Passwords do not match"]);
    exit;
}

// Check email
$check = mysqli_query($conn, "SELECT user_id FROM users WHERE email='$email'");
if (mysqli_num_rows($check) > 0) {
    echo json_encode(["status" => "error", "message" => "Email already exists"]);
    exit;
}

// Hash password
$hashed_password = password_hash($password, PASSWORD_DEFAULT);

// Insert
$query = "INSERT INTO users (name, email, hospital_id, password)
          VALUES ('$name', '$email', '$hospital_id', '$hashed_password')";

if (mysqli_query($conn, $query)) {
    echo json_encode(["status" => "success", "message" => "Registration successful"]);
} else {
    echo json_encode(["status" => "error", "message" => mysqli_error($conn)]);
}
?>
