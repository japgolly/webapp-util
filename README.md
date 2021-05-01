# Webapp Protocols for Scala

This library is extracted (with permission) from the closed-source [ShipReq](https://blog.shipreq.com/about/)
where it when through many evolutions, and was battled-tested on a real-world, large and complex project.
Only the latest code has been ported without the git commit history, but please know that in this case,
the low commit count is not an indication of immaturity.

# Included

* The `core` module:

  * `japgolly.webapp_protocols.core.general`
    * `ErrorMsg` - A typed error message, with some util and predefined cases
    * `EscapeUtil` - functions for escaping strings
    * `Protocol` - Very abstract definitions of protocols
    * `ServerSideProcInvoker` - Abstract and invokable representation of a server-side procedure (JS)
    * `Url` - Types for URLs

  * `japgolly.webapp_protocols.core.ajax`
    * `AjaxProtocol` - Protocol for an AJAX endpoint (JVM & JS)
    * `AjaxClient` - Various means for a client to perform AJAX calls (JS)

  * `japgolly.webapp_protocols.core.binary`
    * `BinaryData` - immutable representation of BinaryData
    * `BinaryJs` - functions for conversion between various JS binary data types

  * `japgolly.webapp_protocols.core.entrypoint`
    * `EntrypointDef` - definition of a JS app entrypoint (JVM & JS)
    * `Entrypoint` - abstract class for a JS app entrypoint (JS)
    * `EntrypointInvoker` - generate JS to invoke an entrypoint (JVM)
    * `LoadJs` - define a bundle of JS assets to be loaded via `loadjs` before entrypoint invocation (JVM)

* The `core-test` module:
  * `japgolly.webapp_protocols.core.test`
    * `TestAjaxClient` - an `AjaxClient` instance for use in tests

* The `circe` module:
  * `japgolly.webapp_protocols.circe`
    * `JsonCodec` - The composition of Circe's `Encoder` and `Decoder` into a single typeclass
    * `JsonUtil` - Various utilities to supplement Circe

* The `circe-test` module:
  * `japgolly.webapp_protocols.circe.test`
    * `JsonTestUtil` - Various utilities to test JSON codecs


# TODO:
* Add ScalaDoc and proper doc
* Boopickle ext
  * entrypoint codec (see ClientSideProcImpl, ClientSideProcInvoker.invokeSB)

