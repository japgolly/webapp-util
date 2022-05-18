package japgolly.webapputil.examples.entrypoint

import japgolly.webapputil.entrypoint._

// Here we demonstrate how a backend web server can generate HTML to have the client
// invoke the entrypoint and start the app.
object Backend {

  private val invoker = EntrypointInvoker(EntrypointExample.defn)

  // It's as simple as this: provide an input value, get HTML.
  //
  // You can expect to see something like this:
  //
  // <script type="text/javascript" src="data:application/javascript;base64,…"></script>
  //
  // which is morally equivalent to:
  //
  // <script>
  //   MyExampleApp(encoded(initialData))
  // </script>
  //
  // We'll look at this in more detail in the next section.
  //
  def generateHtml(i: EntrypointExample.InitialData): Html =
    invoker(i).toHtmlScriptTag

  // The same as above, except instead of having the invocation occur immediately,
  // it schedules it to run on window.onload.
  //
  // This is morally equivalent to:
  //
  // <script>
  //   window.onload = function() {
  //     MyExampleApp(encoded(initialData))
  //   }
  // </script>
  //
  // We'll look at this in more detail in the next section, too.
  //
  def generateHtmlToRunOnWindowLoad(i: EntrypointExample.InitialData): Html =
    invoker(Js.Wrapper.windowOnLoad, i).toHtmlScriptTag
}
