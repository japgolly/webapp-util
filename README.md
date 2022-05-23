# Scala Webapp Utilities
[![Build Status](https://travis-ci.org/japgolly/webapp-util.svg?branch=master)](https://travis-ci.org/japgolly/webapp-util)
[![Latest Version](https://maven-badges.herokuapp.com/maven-central/com.github.japgolly.webapp-util/core_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.japgolly.webapp-util/core_2.13)

This library was initially extracted (with permission) from the closed-source [ShipReq](https://blog.shipreq.com/about/)
where it when through many evolutions, and was battled-tested on a real-world, large and complex project.
It was ported without git commit history, so please understand that in this case,
the low commit count is not an indication of immaturity.

```scala
val WebappUtilVer = "<version>"

// Minimal
"com.github.japgolly.webapp-util" %%% "core"             % WebappUtilVer
"com.github.japgolly.webapp-util" %%% "test"             % WebappUtilVer % Test

// Node-specific additional testing support
"com.github.japgolly.webapp-util" %%% "test-node"        % WebappUtilVer % Test

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

## The `core` module:

* `japgolly.webapputil.general`
  * [`AsyncFunction`](./core/js/src/main/scala/japgolly/webapputil/general/AsyncFunction.scala) - an async, failable function *(JS only)*
  * [`Enabled`](./core/shared/src/main/scala/japgolly/webapputil/general/Enabled.scala) - type-safe union of `Enabled | Disabled`
  * [`ErrorMsg`](./core/shared/src/main/scala/japgolly/webapputil/general/ErrorMsg.scala) - typed error message, with some util and predefined cases
  * [`JsExt`](./core/js/src/main/scala/japgolly/webapputil/general/JsExt.scala) - JS-only implicit extensions *(JS only)*
  * [`LazyVal`](./core/shared/src/main/scala/japgolly/webapputil/general/LazyVal.scala) - lightweight `lazy val` as a portable value
  * [`LoggerJs`](./core/js/src/main/scala/japgolly/webapputil/general/LoggerJs.scala) - simple logger *(JS only)*
  * [`Permission`](./core/shared/src/main/scala/japgolly/webapputil/general/Permission.scala) - type-safe union of `Allow | Deny`
  * [`Protocol`](./core/shared/src/main/scala/japgolly/webapputil/general/Protocol.scala) - abstract definitions of protocols
  * [`Retries`](./core/shared/src/main/scala/japgolly/webapputil/general/Retries.scala) - immutable retry policy
  * [`ThreadUtils`](./core/jvm/src/main/scala/japgolly/webapputil/general/ThreadUtils.scala) - thread groups, thread pools, `ExecutionContext` util, shutdown hooks  *(JVM only)*
  * [`TimersJs`](./core/js/src/main/scala/japgolly/webapputil/general/TimersJs.scala) - API over JS timers *(JS only)*
  * [`Url`](./core/shared/src/main/scala/japgolly/webapputil/general/Url.scala) - types for URLs
  * [`VarJs`](./core/js/src/main/scala/japgolly/webapputil/general/VarJs.scala) - immutable reference to a potentially abstract, potentially mutable variable *(JS only)*
  * [`Version`](./core/shared/src/main/scala/japgolly/webapputil/general/Version.scala) - types for a version with a major and minor component

* `japgolly.webapputil.ajax`
  * [`AjaxProtocol`](./core/shared/src/main/scala/japgolly/webapputil/ajax/AjaxProtocol.scala) - protocol for an AJAX endpoint
  * [`AjaxClient`](./core/js/src/main/scala/japgolly/webapputil/ajax/AjaxClient.scala) - means for a client to perform AJAX calls *(JS only)*

* `japgolly.webapputil.binary`
  * [`BinaryData`](./core/shared/src/main/scala/japgolly/webapputil/binary/BinaryData.scala) - immutable representation of BinaryData
  * [`BinaryFormat`](./core/js/src/main/scala/japgolly/webapputil/binary/BinaryFormat.scala) - converts a type to a binary format and back *(JS only)*
  * [`BinaryJs`](./core/js/src/main/scala/japgolly/webapputil/binary/BinaryJs.scala) - functions for conversion between various JS binary data types *(JS only)*
  * [`BinaryString`](./core/js/src/main/scala/japgolly/webapputil/binary/BinaryString.scala) - binary data efficiently encoded as a UTF-16 string *(JS only)*
  * [`CodecEngine`](./core/shared/src/main/scala/japgolly/webapputil/binary/CodecEngine.scala) - capability to encode and decode binary data given a codec typeclass
  * [`Compression`](./core/js/src/main/scala/japgolly/webapputil/binary/Compression.scala) - binary compression and decompression *(JS only)*
  * [`Encryption`](./core/js/src/main/scala/japgolly/webapputil/binary/Encryption.scala) - binary encryption and decryption *(JS only)*
  * [`Pako`](./core/js/src/main/scala/japgolly/webapputil/binary/Pako.scala) - facade for the JS `pako` library with provides zlib compression & decompression *(JS only)*

* `japgolly.webapputil.browser`
  * [`WindowConfirm`](./core/js/src/main/scala/japgolly/webapputil/browser/WindowConfirm.scala) - abstraction over `window.confirm` *(JS only)*
  * [`WindowLocation`](./core/js/src/main/scala/japgolly/webapputil/browser/WindowLocation.scala) - abstraction over `window.location` *(JS only)*
  * [`WindowPrompt`](./core/js/src/main/scala/japgolly/webapputil/browser/WindowPrompt.scala) - abstraction over `window.prompt` *(JS only)*

* `japgolly.webapputil.entrypoint`
  * [`EntrypointDef`](./core/shared/src/main/scala/japgolly/webapputil/entrypoint/EntrypointDef.scala) - definition of a JS app entrypoint
  * [`Entrypoint`](./core/js/src/main/scala/japgolly/webapputil/entrypoint/Entrypoint.scala) - abstract class for a JS app entrypoint *(JS only)*
  * [`EntrypointInvoker`](./core/jvm/src/main/scala/japgolly/webapputil/entrypoint/EntrypointInvoker.scala) - generate JS to invoke an entrypoint *(JVM only)*
  * [`Html`](./core/jvm/src/main/scala/japgolly/webapputil/entrypoint/Html.scala) - HTML content *(JVM only)*
  * [`Js`](./core/jvm/src/main/scala/japgolly/webapputil/entrypoint/Js.scala) - typed text representing JavaScript code (plus some util) *(JVM only)*
  * [`LoadJs`](./core/jvm/src/main/scala/japgolly/webapputil/entrypoint/LoadJs.scala) - define a bundle of JS assets to be loaded via `loadjs` before entrypoint invocation *(JVM only)*

* `japgolly.webapputil.http`
  * [`Cookie`](./core/shared/src/main/scala/japgolly/webapputil/http/Cookie.scala) - abstract HTTP cookie and associated util
  * [`HttpClient`](./core/shared/src/main/scala/japgolly/webapputil/http/HttpClient.scala) - abstract HTTP (invocation) client
  * `UrlEncoder` - cross-platform URL encoding and decoding

* `japgolly.webapputil.indexeddb` *(JS only)*
  * [`IndexedDb`](./core/js/src/main/scala/japgolly/webapputil/indexeddb/IndexedDb.scala) - monadic API over IndexedDb that enforces transaction rules at compile-time, and provides higher-level ops such as atomic async modification *(JS only)*
  * [`IndexedDbKey`](./core/js/src/main/scala/japgolly/webapputil/indexeddb/IndexedDbKey.scala) - typed key for use in IndexedDb *(JS only)*
  * [`KeyCodec`](./core/js/src/main/scala/japgolly/webapputil/indexeddb/KeyCodec.scala) - codec between an arbitrary type and a IndexedDb key *(JS only)*
  * [`ObjectStoreDef`](./core/js/src/main/scala/japgolly/webapputil/indexeddb/ObjectStoreDef.scala) - IndexedDb store and codecs *(JS only)*
  * [`ValueCodec`](./core/js/src/main/scala/japgolly/webapputil/indexeddb/ValueCodec.scala) - codec between an arbitrary type and a IndexedDb value *(JS only)*

* `japgolly.webapputil.locks`
  * [`LockMechanism`](./core/jvm/src/main/scala/japgolly/webapputil/locks/LockMechanism.scala) - means of implicitly specifying how to acquire locks *(JVM only)*
  * [`LockUtils`](./core/jvm/src/main/scala/japgolly/webapputil/locks/LockUtils.scala) - helpers around Java locks *(JVM only)*
  * [`GenericSharedLock.Safe`](./core/shared/src/main/scala/japgolly/webapputil/locks/GenericSharedLock.scala) - shared lock APIs that are FP-safe (consistent between JVM and JS)
  * [`GenericSharedLock.Unsafe`](./core/shared/src/main/scala/japgolly/webapputil/locks/GenericSharedLock.scala) - shared lock APIs that are FP-unsafe, in that not effect type is used (consistent between JVM and JS)
  * `SharedLock` - lock that can be safely shared between threads *(different API between JVM & JS)*
  * `SharedLock.ReadWrite` - read/write lock that can be safely shared between threads *(different API between JVM & JS)*

* `japgolly.webapputil.websocket`
  * [`WebSocket`](./core/js/src/main/scala/japgolly/webapputil/websocket/WebSocket.scala) - abstract API over a websocket connection *(JS only)*
  * [`WebSocketClient`](./core/js/src/main/scala/japgolly/webapputil/websocket/WebSocketClient.scala) - high-level, managed websocket connection from client to server, supporting things like authorisation, auto-reconnection, retries, session expiry ([`TLA+ spec`](./TLA+/websocket_client.tla)) *(JS only)*
  * [`WebSocketServerUtil`](./core/jvm/src/main/scala/japgolly/webapputil/websocket/WebSocketServerUtil.scala) - util for writing server-side websockets *(JVM only)*
  * [`WebSocketShared`](./core/shared/src/main/scala/japgolly/webapputil/websocket/WebSocketShared.scala) - definitions and util shared between websocket client and server

* `japgolly.webapputil.webstorage` *(JS only)*
  * [`AbstractWebStorage`](./core/js/src/main/scala/japgolly/webapputil/webstorage/AbstractWebStorage.scala) - API over webstorage with some impls
  * [`KeyCodec`](./core/js/src/main/scala/japgolly/webapputil/indexeddb/KeyCodec.scala) - codec between an arbitrary type and a webstorage key
  * [`ValueCodec`](./core/js/src/main/scala/japgolly/webapputil/indexeddb/ValueCodec.scala) - codec between an arbitrary type and a webstorage value
  * [`WebStorageKey`](./core/js/src/main/scala/japgolly/webapputil/webstorage/WebStorageKey.scala) - high-level interface to data in webstorage

* `japgolly.webapputil.webworker` *(JS only)*
  * [`AbstractWebWorker`](./core/js/src/main/scala/japgolly/webapputil/webworker/AbstractWebWorker.scala) - web worker client & server API
  * [`ManagedWebWorker`](./core/js/src/main/scala/japgolly/webapputil/webworker/ManagedWebWorker.scala) - web worker client & server implementations that handle all the low-level work
  * [`OnError`](./core/js/src/main/scala/japgolly/webapputil/webworker/OnError.scala) - web worker error handler
  * [`WebWorkerProtocol`](./core/js/src/main/scala/japgolly/webapputil/webworker/WebWorkerProtocol.scala) - protocol API for communication between web worker client and server

## The `test` module:

* `japgolly.webapputil.test`
  * [`BinaryTestUtil`](./testCore/shared/src/main/scala/japgolly/webapputil/test/BinaryTestUtil.scala) - util for testing binary data
  * [`TestAjaxClient`](./testCore/js/src/main/scala/japgolly/webapputil/test/TestAjaxClient.scala) - `AjaxClient` instance for use in tests *(JS only)*
  * [`TestHttpClient`](./testCore/shared/src/main/scala/japgolly/webapputil/test/TestHttpClient.scala) - `HttpClient` instance for use in tests
  * [`TestTimersJs`](./testCore/js/src/main/scala/japgolly/webapputil/test/TestTimersJs.scala) - `TimersJs` instance for use in tests *(JS only)*
  * [`TestWebSocket`](./testCore/js/src/main/scala/japgolly/webapputil/test/TestWebSocket.scala) - `WebSocket` instance for use in tests *(JS only)*
  * [`TestWebWorker`](./testCore/js/src/main/scala/japgolly/webapputil/test/TestWebWorker.scala) - in-memory instances of `AbstractWebWorker` client and server API for use in tests *(JS only)*
  * [`TestWindowConfirm`](./testCore/js/src/main/scala/japgolly/webapputil/test/TestWindowConfirm.scala) - `WindowConfirm` instance for use in tests *(JS only)*
  * [`TestWindowLocation`](./testCore/js/src/main/scala/japgolly/webapputil/test/TestWindowLocation.scala) - `WindowLocation` instance for use in tests *(JS only)*
  * [`TestWindowPrompt`](./testCore/js/src/main/scala/japgolly/webapputil/test/TestWindowPrompt.scala) - `WindowPrompt` instance for use in tests *(JS only)*

## The `test-node` module:

* `japgolly.webapputil.test.node` *(JS only)*
  * [`TestNode`](./testNode/src/main/scala/japgolly/webapputil/test/node/TestNode.scala) - util for testing using Node

## The `core-boopickle` module:

* `japgolly.webapputil.boopickle`
  * [`BinaryFormatExt`](./coreBoopickle/js/src/main/scala/japgolly/webapputil/boopickle/BinaryFormatExt.scala) - additional functionality around `BinaryFormat` *(JS only)*
  * [`BinaryWebWorkerProtocol`](./coreBoopickle/js/src/main/scala/japgolly/webapputil/boopickle/BinaryWebWorkerProtocol.scala) - implementation of `WebWorkerProtocol` that uses boopickle for message encoding *(JS only)*
  * [`BoopickleCodecEngine`](./coreBoopickle/shared/src/main/scala/japgolly/webapputil/boopickle/BoopickleCodecEngine.scala) - implementation of `CodecEngine` for boopickle
  * [`BoopickleWebSocketClient`](./coreBoopickle/js/src/main/scala/japgolly/webapputil/boopickle/BoopickleWebSocketClient.scala) - implementation of `WebSocketClient` that uses boopickle for message encoding *(JS only)*
  * [`EncryptionEngine`](./coreBoopickle/js/src/main/scala/japgolly/webapputil/boopickle/EncryptionEngine.scala) - implementation of `Encryption.Engine` *(JS only)*
  * [`EntrypointDefExt`](./coreBoopickle/shared/src/main/scala/japgolly/webapputil/boopickle/EntrypointDefExt.scala) - additional functionality around `EntrypointDef`
  * [`IndexedDbExt`](./coreBoopickle/js/src/main/scala/japgolly/webapputil/boopickle/IndexedDbExt.scala) - additional functionality around IndexedDb *(JS only)*
  * [`PicklerUtil`](./coreBoopickle/shared/src/main/scala/japgolly/webapputil/boopickle/PicklerUtil.scala) - util around, and implementations of, Boopickle `Pickler`s
  * [`SafePickler`](./coreBoopickle/shared/src/main/scala/japgolly/webapputil/boopickle/SafePickler.scala) - safer version of a Boopickle `Pickler` with versioning support
  * [`SafePicklerUtil`](./coreBoopickle/shared/src/main/scala/japgolly/webapputil/boopickle/SafePicklerUtil.scala) - util for working with `SafePickler` versions during (de)serialisation

## The `test-boopickle` module:

* `japgolly.webapputil.boopickle.test`
  * [`TestEncryption`](./testBoopickle/js/src/main/scala/japgolly/webapputil/boopickle/test/TestEncryption.scala) - util for testing encryption *(JS only)*
  * [`TestIndexedDb`](./testBoopickle/js/src/main/scala/japgolly/webapputil/boopickle/test/TestIndexedDb.scala) - util for testing IndexedDb code *(JS only)*
  * [`WebSocketTestUtil`](./testBoopickle/js/src/main/scala/japgolly/webapputil/boopickle/test/WebSocketTestUtil.scala) - util for testing WebSocket code *(JS only)*

## The `core-cats-effect` module:
* `japgolly.webapputil.cats.effect`
  * Implicits so that `IO` is recognised as an effect type usable by the rest of the this library.
    * `Effect.Async[IO]`
    * `Effect.Sync[IO]` *(JVM only)*
  * [`ThreadUtilsIO`](./coreCatsEffect/jvm/src/main/scala/japgolly/webapputil/cats/effect/ThreadUtilsIO.scala) - thread pools, scheduling, shutdown hooks, `IO` runtimes *(JVM only)*

## The `test-cats-effect` module:

* `japgolly.webapputil.cats.effect.test`
  * [`TestHttpClientIO`](./testCatsEffect/jvm/src/main/scala/japgolly/webapputil/cats/effect/test/package.scala) *(JVM only)*

## The `core-circe` module:

* `japgolly.webapputil.circe`
  * [`JsonAjaxClient`](./coreCirce/js/src/main/scala/japgolly/webapputil/circe/JsonAjaxClientModule.scala) - implementation of `AjaxClient` that uses JSON and `JsonCodec` *(JS only)*
  * [`JsonCodec`](./coreCirce/shared/src/main/scala/japgolly/webapputil/circe/JsonCodec.scala) - composition of Circe's `Encoder` and `Decoder` into a single typeclass
  * [`JsonEntrypointCodec`](./coreCirce/shared/src/main/scala/japgolly/webapputil/circe/JsonEntrypointCodec.scala) - creates instances of `EntrypointDef.Codec` using Circe codecs
  * [`JsonUtil`](./coreCirce/shared/src/main/scala/japgolly/webapputil/circe/JsonUtil.scala) - util to supplement Circe
  * Extension methods available via `import japgolly.webapputil.circe._`
    * `HttpClient.Body.json` to create response bodies as JSON
    * `HttpClient.Body#parseJsonBody` to parse request bodies as JSON

## The `test-circe` module:

* `japgolly.webapputil.circe.test`
  * [`JsonTestUtil`](./testCirce/shared/src/main/scala/japgolly/webapputil/circe/test/JsonTestUtil.scala) - util to test JSON codecs
  * [`TestJsonAjaxClient`](./testCirce/js/src/main/scala/japgolly/webapputil/circe/test/package.scala) - implementation of `TestAjaxClient` that uses JSON and `JsonCodec` *(JS only)*

## The `core-okhttp4` module: *(JVM only)*

* `japgolly.webapputil.okhttp4`
  * [`OkHttp4Client`](./coreOkHttp4/src/main/scala/japgolly/webapputil/okhttp4/OkHttp4Client.scala) - implementation of `HttpClient` using okhttp4

## The `db-postgres` module: *(JVM only)*

* `japgolly.webapputil.db`
  * [`Db`](./dbPostgres/src/main/scala/japgolly/webapputil/db/Db.scala) - connection to the database
  * [`DbConfig`](./dbPostgres/src/main/scala/japgolly/webapputil/db/DbConfig.scala) - DB config definitions, i.e. to load DB details at runtime on app startup
  * [`DbMigration`](./dbPostgres/src/main/scala/japgolly/webapputil/db/DbMigration.scala) - manages DB schema migrations (via [Flyway](https://flywaydb.org))
  * [`DoobieCodecs`](./dbPostgres/src/main/scala/japgolly/webapputil/db/DoobieCodecs.scala) - a few generic codecs for Doobie
  * [`DoobieHelpers`](./dbPostgres/src/main/scala/japgolly/webapputil/db/DoobieHelpers.scala) - helpers for Doobie
  * [`XA`](./dbPostgres/src/main/scala/japgolly/webapputil/db/XA.scala) - wrapper around `Transactor[IO]` (used to be more and may again)

## The `test-db-postgres` module: *(JVM only)*

* `japgolly.webapputil.db.test`
  * [`DbTable`](./testDbPostgres/src/main/scala/japgolly/webapputil/db/test/DbTable.scala) - util around DB tables and row counting
  * [`TestDb`](./testDbPostgres/src/main/scala/japgolly/webapputil/db/test/TestDb.scala) - provides access to a test DB, manages things like migration, and util
  * [`TestXA`](./testDbPostgres/src/main/scala/japgolly/webapputil/db/test/TestXA.scala) - a live connection to the DB, and util to make testing as easy as possible


# TODO:

* Add examples
* Add ScalaDoc and proper doc


# Support
If you like what I do
—my OSS libraries, my contributions to other OSS libs, [my programming blog](https://japgolly.blogspot.com)—
and you'd like to support me, more content, more lib maintenance, [please become a patron](https://www.patreon.com/japgolly)!
I do all my OSS work unpaid so showing your support will make a big difference.
