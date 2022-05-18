package japgolly.webapputil.examples.entrypoint

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.webapputil.entrypoint._
import japgolly.webapputil.examples.entrypoint.EntrypointExample.InitialData
import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel(EntrypointExample.Name) // Instruct Scala.js to make this available in
                                          // the global JS namespace.
object Frontend extends Entrypoint(EntrypointExample.defn) {

  // Because the line above extends Entrypoint, all we need to do now is implement a
  // run method that takes the decoded InitialData value.
  override def run(i: InitialData): Unit = {
    // Render a simple React component
    val reactApp = Component(i)
    reactApp.renderIntoDOM(`#root`) // `#root` is a helper to find DOM with id=root
  }

  val Component = ScalaComponent.builder[InitialData]
    .render_P { i => <.div(s"Hello @${i.username} and nice to meet you!") }
    .build
}
