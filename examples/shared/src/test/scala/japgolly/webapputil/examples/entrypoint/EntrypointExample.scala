package japgolly.webapputil.examples.entrypoint

import boopickle.DefaultBasic._
import japgolly.webapputil.boopickle._
import japgolly.webapputil.entrypoint._

object EntrypointExample {

  // The name of our app, as it will appear in the JS global namespace when loaded.
  // This is final because its referenced via @JSExportTopLevel on Frontend
  final val Name = "MyExampleApp"

  // In this example, our app will request the user's username as soon as it starts up.
  // The server will provide this to the client.
  final case class InitialData(username: String)

  // This is our codec typeclass from InitialData to binary and back
  implicit val picklerInitialData: Pickler[InitialData] =
    implicitly[Pickler[String]].xmap(InitialData.apply)(_.username)

  // Finally, our entrypoint definition.
  //
  // The Pickler[InitialData] we created above is pulled in implicitly.
  // (Note: Binary is just one supported format, and not at all a necessity.)
  //
  val defn = EntrypointDef[InitialData](Name)
}
