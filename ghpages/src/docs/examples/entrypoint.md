{% laika.title = Entrypoints %}

Entrypoint Example
==================

An `Entrypoint` is a method on the client-side, that you must call in order to start your webapp.

A typical webapp will be served like this:

```html
  <script type="text/javascript" src="/my-app.js"></script>
  <script type="text/javascript">
    MyExampleApp.start();
  </script>
```

## Why?

Generating this stuff manually isn't a huge deal, but in this example we'll use the `Entrypoint` API
for a few advantages:

  * Everything is DRY — no chance to accidentally call the wrong function
  * The server can provide custom data to initialise our app — just need to provide a codec, ser/deser and JS plumbing handled automatically
  * We avoid a typical AJAX call to initialise our app — faster and better experience for users
  * HTML is generated for us — things like escaping handled automatically

## Shared Definition

We'll start with our entrypoint definition, which is cross-compiled for JVM and JS.

@:sourceFile(EntrypointExample.scala)

## Frontend

Next we'll create our Scala.js frontend.

@:sourceFile(EntrypointExampleFrontend.scala)

## Backend

Because we initialise our webapp with a username, the backend server needs to generate different HTML depending on who
the request is for.

A few important things are out of scope for this demo:

  * how to retrieve a username depends on your app
  * how to serve HTML depends on your choice of web server
  * how to serve the frontend JS depends on your app and your choice of web server!

In our example below we simply focus on `InitialData => Html`.

@:sourceFile(EntrypointExampleBackend.scala)

## Backend Test

The only real value in this test is that our `Pickler[InitialData]` serialises and deserialises correctly.
Everything else is just to demonstrate how exactly how the generated HTML works.

@:sourceFile(EntrypointExampleBackendTest.scala)
