<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>http4k passkeys - sign up</title>
    <#include "style.ftl">
</head>
<body>
    <h1>Create your account</h1>
    <p>Passwordless sign-up: your passkey is the only credential.</p>
    <p><input id="email" type="email" placeholder="email" value="alice@example.com"></p>
    <p><input id="firstName" placeholder="first name" value="Alice"></p>
    <p><input id="lastName" placeholder="last name" value="Smith"></p>
    <p><button onclick="signup()">Create account with a passkey</button></p>
    <hr>
    <p>Already have a passkey? <button onclick="authenticate()">Sign in</button></p>
    <div id="log"></div>
    <#include "passkeyscript.ftl">
</body>
</html>
