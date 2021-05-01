# Scala Webapp Utilities

This library was initially extracted (with permission) from the closed-source [ShipReq](https://blog.shipreq.com/about/)
where it when through many evolutions, and was battled-tested on a real-world, large and complex project.
It was ported without git commit history, so please understand that in this case,
the low commit count is not an indication of immaturity.

# Included

* The `core` module:

  * `japgolly.webapputil.core.general`
    * `ErrorMsg` - typed error message, with some util and predefined cases
    * `EscapeUtil` - functions for escaping strings
    * `Protocol` - abstract definitions of protocols
    * `ServerSideProcInvoker` - abstract and invokable representation of a server-side procedure (JS)
    * `Url` - types for URLs

  * `japgolly.webapputil.core.ajax`
    * `AjaxProtocol` - protocol for an AJAX endpoint (JVM & JS)
    * `AjaxClient` - means for a client to perform AJAX calls (JS)

  * `japgolly.webapputil.core.binary`
    * `BinaryData` - immutable representation of BinaryData
    * `BinaryJs` - functions for conversion between various JS binary data types

  * `japgolly.webapputil.core.entrypoint`
    * `EntrypointDef` - definition of a JS app entrypoint (JVM & JS)
    * `Entrypoint` - abstract class for a JS app entrypoint (JS)
    * `EntrypointInvoker` - generate JS to invoke an entrypoint (JVM)
    * `LoadJs` - define a bundle of JS assets to be loaded via `loadjs` before entrypoint invocation (JVM)

* The `core-test` module:
  * `japgolly.webapputil.core.test`
    * `BinaryTestUtil` - utilities for testing binary data (JVM & JS)
    * `TestAjaxClient` - an `AjaxClient` instance for use in tests (JS)

* The `circe` module:
  * `japgolly.webapputil.circe`
    * `JsonCodec` - composition of Circe's `Encoder` and `Decoder` into a single typeclass
    * `JsonEntrypointCodec` - creates instances of `EntrypointDef.Codec` using Circe codecs
    * `JsonUtil` - utilities to supplement Circe

* The `circe-test` module:
  * `japgolly.webapputil.circe.test`
    * `JsonTestUtil` - utilities to test JSON codecs


# TODO:
* Add ScalaDoc and proper doc
