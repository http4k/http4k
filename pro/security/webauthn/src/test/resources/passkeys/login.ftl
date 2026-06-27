<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>http4k passkeys - sign in</title>
    <#include "style.ftl">
</head>
<body>
    <h1>Sign in</h1>
    <form method="post" action="/login/password">
        <p><input name="username" placeholder="username" value="alice"></p>
        <p><input type="password" name="password" placeholder="password" value="hunter2"></p>
        <p><button type="submit">Log in with password</button></p>
    </form>
    <p>or <button onclick="authenticate()">Sign in with a passkey</button></p>
    <p>No account? <a href="/signup">Sign up with a passkey</a></p>
    <div id="log"></div>
    <#include "passkeyscript.ftl">
</body>
</html>
