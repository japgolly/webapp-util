package japgolly.webapputil.protocol.entrypoint

final case class Js(val asString: String) extends AnyVal {
  // def asXml = <script type="text/javascript">{asString(i)}</script>
}

object Js {

  final case class Wrapper(before: String, after: String) {
    val totalLength = before.length + after.length

    def around(inside: Wrapper): Wrapper =
      Wrapper(
        before = this.before + inside.before,
        after  = inside.after + this.after,
      )

    def inside(outer: Wrapper): Wrapper =
      outer.around(this)
  }

  object Wrapper {
    val windowOnLoad = Wrapper("window.onload=function(){", "};") // why'd I add a semi-colon here again? Can't remember...
  }
}