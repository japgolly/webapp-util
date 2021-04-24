# Webapp Protocols for Scala

This library is extracted (with permission) from the closed-source [ShipReq](https://blog.shipreq.com/about/)
where it when through many evolutions, and was battled-tested on a real-world, large and complex project.
Only the latest code has been ported without the git commit history, but please know that in this case,
the low commit count is not an indication of immaturity.

# Included

* The `core` module:

  * `japgolly.webapp_protocols.core`
    * `EscapeUtil` - functions for escaping strings

  * `japgolly.webapp_protocols.core.binary`
    * `BinaryData` - immutable representation of BinaryData
    * `BinaryJs` - functions for conversion between various JS binary data types

  * `japgolly.webapp_protocols.core.entrypoint`
    * `EntrypointDef` - definition of a JS app entrypoint (JVM & JS)
    * `Entrypoint` - abstract class for a JS app entrypoint (JS)
    * `EntrypointInvoker` - generate JS to invoke an entrypoint (JVM)


# TODO:
* Add ScalaDoc and proper doc
* Boopickle ext
  * entrypoint codec (see ClientSideProcImpl, ClientSideProcInvoker.invokeSB)
