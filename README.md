# Scala Webapp Utilities
[![Build Status](https://travis-ci.org/japgolly/webapp-util.svg?branch=master)](https://travis-ci.org/japgolly/webapp-util)
[![Latest Version](https://maven-badges.herokuapp.com/maven-central/com.github.japgolly.webapp-util/protocol_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.japgolly.webapp-util/protocol_2.13)

This library was initially extracted (with permission) from the closed-source [ShipReq](https://blog.shipreq.com/about/)
where it when through many evolutions, and was battled-tested on a real-world, large and complex project.
It was ported without git commit history, so please understand that in this case,
the low commit count is not an indication of immaturity.

```scala
val WebappUtilVer = "<version>"

// Minimal
"com.github.japgolly.webapp-util" %%% "core"             % WebappUtilVer
"com.github.japgolly.webapp-util" %%% "test"             % WebappUtilVer % Test

// Requiring Boopickle
"com.github.japgolly.webapp-util" %%% "core-boopickle"   % WebappUtilVer
"com.github.japgolly.webapp-util" %%% "test-boopickle"   % WebappUtilVer % Test

// Cats Effect support
"com.github.japgolly.webapp-util" %%% "core-cats-effect" % WebappUtilVer
"com.github.japgolly.webapp-util" %%% "test-cats-effect" % WebappUtilVer % Test

// Circe JSON support
"com.github.japgolly.webapp-util" %%% "core-circe"       % WebappUtilVer
"com.github.japgolly.webapp-util" %%% "test-circe"       % WebappUtilVer % Test

// HttpClient implementation using okhttp4
"com.github.japgolly.webapp-util"  %% "core-okhttp4"     % WebappUtilVer

// Postgres support via Doobie and Cats Effect
"com.github.japgolly.webapp-util"  %% "db-postgres"      % WebappUtilVer
"com.github.japgolly.webapp-util"  %% "test-db-postgres" % WebappUtilVer % Test
```


# Included

* The `core` module:

  * `japgolly.webapputil.general`
    * `ErrorMsg` - typed error message, with some util and predefined cases
    * `EscapeUtil` - functions for escaping strings
    * `LazyVal` - A lightweight `lazy val` as a portable value
    * `LoggerJs` - A simple logger *(JS only)*
    * `Protocol` - abstract definitions of protocols
    * `ServerSideProcInvoker` - abstract and invokable representation of a server-side procedure *(JS only)*
    * `ThreadUtils` - thread groups, thread pools, `ExecutionContext` util, shutdown hooks  *(JVM only)*
    * `Url` - types for URLs

  * `japgolly.webapputil.ajax`
    * `AjaxProtocol` - protocol for an AJAX endpoint
    * `AjaxClient` - means for a client to perform AJAX calls *(JS only)*

  * `japgolly.webapputil.binary`
    * `BinaryData` - immutable representation of BinaryData
    * `BinaryJs` - functions for conversion between various JS binary data types *(JS only)*

  * `japgolly.webapputil.browser`
    * `WindowConfirm` - Abstraction over `window.confirm` *(JS only)*
    * `WindowLocation` - Abstraction over `window.location` *(JS only)*
    * `WindowPrompt` - Abstraction over `window.prompt` *(JS only)*

  * `japgolly.webapputil.entrypoint`
    * `EntrypointDef` - definition of a JS app entrypoint
    * `Entrypoint` - abstract class for a JS app entrypoint *(JS only)*
    * `EntrypointInvoker` - generate JS to invoke an entrypoint *(JVM only)*
    * `Html` - HTML content *(JVM only)*
    * `Js` - JavaScript code (and some utilities) *(JVM only)*
    * `LoadJs` - define a bundle of JS assets to be loaded via `loadjs` before entrypoint invocation *(JVM only)*

  * `japgolly.webapputil.http`
    * `HttpClient` - an abstract HTTP (invocation) client
    * `UrlEncoder` - cross-platform URL encoding and decoding

  * `japgolly.webapputil.locks`
    * `LockMechanism` - means of implicitly specifying how to acquire locks *(JVM only)*
    * `LockUtils` - helpers around Java locks *(JVM only)*
    * `GenericSharedLock.Safe` - shared lock APIs that are FP-safe (consistent between JVM and JS)
    * `GenericSharedLock.Unsafe` - shared lock APIs that are FP-unsafe, in that not effect type is used (consistent between JVM and JS)
    * `SharedLock` - a lock that can be safely shared between threads *(different API between JVM & JS)*
    * `SharedLock.ReadWrite` - a read/write lock that can be safely shared between threads *(different API between JVM & JS)*

  * `japgolly.webapputil.webworker`
    * `AbstractWebWorker` - web worker client & server API *(JS only)*
    * `ManagedWebWorker` - web worker client & server implementations that handle all the low-level work *(JS only)*
    * `OnError` - web worker error handler *(JS only)*
    * `WebWorkerProtocol` - protocol API for communication between web worker client and server *(JS only)*

* The `test` module:
  * `japgolly.webapputil.test`
    * `BinaryTestUtil` - utilities for testing binary data
    * `TestAjaxClient` - an `AjaxClient` instance for use in tests *(JS only)*
    * `TestHttpClient` - a `HttpClient` instance for use in tests
    * `TestWebWorker` - in-memory instances of `AbstractWebWorker` client and server API for use in tests *(JS only)*
    * `TestWindowConfirm` - a `WindowConfirm` instance for use in tests *(JS only)*
    * `TestWindowLocation` - a `WindowLocation` instance for use in tests *(JS only)*
    * `TestWindowPrompt` - a `WindowPrompt` instance for use in tests *(JS only)*

* The `core-boopickle` module:
  * `japgolly.webapputil.boopickle.webworker`
    * `BinaryWebWorkerProtocol` - implementation of `WebWorkerProtocol` that uses boopickle for message encoding *(JS only)*

* The `test-boopickle` module:
  * nothing yet

* The `core-cats-effect` module:
  * `japgolly.webapputil.cats.effect`
    * Implicits so that `IO` is recognised as an effect type usable by the rest of the this library.
      * `Effect.Async[IO]`
      * `Effect.Sync[IO]` *(JVM only)*
    * `ThreadUtilsIO` - thread pools, scheduling, shutdown hooks, `IO` runtimes *(JVM only)*

* The `test-cats-effect` module:
  * `japgolly.webapputil.cats.effect.test`
    * `TestHttpClientIO` *(JVM only)*

* The `core-circe` module:
  * `japgolly.webapputil.circe`
    * `JsonAjaxClient` - implementation of `AjaxClient` that uses JSON and `JsonCodec` *(JS only)*
    * `JsonCodec` - composition of Circe's `Encoder` and `Decoder` into a single typeclass
    * `JsonEntrypointCodec` - creates instances of `EntrypointDef.Codec` using Circe codecs
    * `JsonUtil` - utilities to supplement Circe
    * Extension methods available via `import japgolly.webapputil.circe._`
      * `HttpClient.Body.json` to create response bodies as JSON
      * `HttpClient.Body#parseJsonBody` to parse request bodies as JSON

* The `test-circe` module:
  * `japgolly.webapputil.circe.test`
    * `JsonTestUtil` - utilities to test JSON codecs
    * `TestJsonAjaxClient` - implementation of `TestAjaxClient` that uses JSON and `JsonCodec` *(JS only)*

* The `core-okhttp4` module: *(JVM only)*
  * `japgolly.webapputil.okhttp4`
    * `OkHttp4Client` - an implementation of `HttpClient` using okhttp4

* The `db-postgres` module: *(JVM only)*
  * `japgolly.webapputil.db`
    * `Db` - connection to the database
    * `DbConfig` - DB config definitions, i.e. to load DB details at runtime on app startup
    * `DbMigration` - manages DB schema migrations (via [Flyway](https://flywaydb.org))
    * `DoobieCodecs` - a few generic codecs for Doobie
    * `DoobieHelpers` - helpers for Doobie
    * `XA` - wrapper around `Transactor[IO]` (used to be more and may again)

* The `test-db-postgres` module: *(JVM only)*
  * `japgolly.webapputil.db.test`
    * `DbTable` - utilities around DB tables and row counting
    * `TestDb` - provides access to a test DB, manages things like migration, and utilities
    * `TestXA` - a live connection to the DB, and utilities to make testing as easy as possible

# TODO:

* Add ScalaDoc and proper doc
* Port IDB stuff
* Port crypto stuff
* Port websocket stuff
* Port webstorage stuff
* Port SafePickler and related


# Support
If you like what I do
—my OSS libraries, my contributions to other OSS libs, [my programming blog](https://japgolly.blogspot.com)—
and you'd like to support me, more content, more lib maintenance, [please become a patron](https://www.patreon.com/japgolly)!
I do all my OSS work unpaid so showing your support will make a big difference.
