package japgolly.webapputil.entrypoint

import org.scalajs.dom
import scala.scalajs.js.annotation.JSExport

abstract class Entrypoint[Input](final val defn: EntrypointDef[Input]) {

  @JSExport(EntrypointDef.MainMethodName)
  final def main(encodedInput: String): Unit =
    run(decodeInput(encodedInput))

  final def decodeInput(s: String): Input =
    defn.codec.decodeOrThrow(s)

  @inline final protected def `#root` = dom.document.getElementById("root")
  @inline final protected def `#main` = dom.document.getElementById("main")

  def run(i: Input): Unit
}
