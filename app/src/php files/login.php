<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

include "db.php";

// Read JSON input
$data = json_decode(file_get_contents("php://input"), true);

if (!$data) {
    echo json_encode([
        "status" => "error",
        "message" => "Invalid JSON data"
    ]);
    exit;
}

$email    = $data['email'] ?? '';
$password = $data['password'] ?? '';

// Validation
if ($email == "" || $password == "") {
    echo json_encode([
        "status" => "error",
        "message" => "Email and password are required"
    ]);
    exit;
}

// Check user
$query = "SELECT user_id, name, email, hospital_id, password, role 
          FROM users 
          WHERE email = '$email'";

$result = mysqli_query($conn, $query);

if (mysqli_num_rows($result) == 0) {
    echo json_encode([
        "status" => "error",
        "message" => "Invalid email or password"
    ]);
    exit;
}

$user = mysqli_fetch_assoc($result);

// Verify password
if (!password_verify($password, $user['password'])) {
    echo json_encode([
        "status" => "error",
        "message" => "Invalid email or password"
    ]);
    exit;
}

// Success
echo json_encode([
    "status" => "success",
    "message" => "Login successful",
    "user" => [
        "user_id"     => $user['user_id'],
        "name"        => $user['name'],
        "email"       => $user['email'],
        "hospital_id" => $user['hospital_id'],
        "role"        => $user['role']
    ]
]);
?>
