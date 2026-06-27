<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>protected</title>
    <#include "style.ftl">
</head>
<body>
    <h1>Signed in as ${user}</h1>
    <p><button onclick="register()">Add a passkey</button> &middot; <a href="/logout">Log out</a></p>
    <div id="log"></div>
    <#include "passkeyscript.ftl">
</body>
</html>
