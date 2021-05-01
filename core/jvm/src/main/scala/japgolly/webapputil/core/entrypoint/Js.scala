package japgolly.webapputil.core.entrypoint

final case class Js(val asString: String) extends AnyVal {
  // def asXml = <script type="text/javascript">{asString(i)}</script>
}

object Js {
  final case class Wrapper(before: String, after: String) {
    val totalLength = before.length + after.length
  }
  object Wrapper {
    val onLoad = Wrapper("window.onload=function(){", "};") // why'd I add a semi-colon here again? Can't remember...
  }
}