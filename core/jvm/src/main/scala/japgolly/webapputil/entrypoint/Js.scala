package japgolly.webapputil.entrypoint

import japgolly.microlibs.stdlib_ext.EscapeUtils
import japgolly.univeq.UnivEq
import japgolly.webapputil.binary.BinaryData
import java.lang.{StringBuilder => JStringBuilder}

final case class Js(val asString: String) extends AnyVal {
    import Js.Const._

  @inline def toHtmlScriptTag: Html =
    scriptInlineBase64

  /** Create a script tag to execute this JS.
    *
    * Example:
    * {{{
    *   <script type="text/javascript" src="data:application/javascript;base64,Y29uc29sZS5sb2coJzwvc2NyaXB0Picp"></script>
    * }}}
    */
  def scriptInlineBase64: Html =
    Html(
      """<script type="text/javascript" src="data:application/javascript;base64,""" +
      BinaryData.fromStringAsUtf8(asString).toBase64 +
      """"></script>"""
    )

  /** Create a script tag to execute this JS.
    *
    * Example:
    * {{{
    *   <script type="text/javascript" src="data:application/javascript,var%20x%20=%20'%3C/script%3E';%20alert(x)"></script>
    * }}}
    */
  def scriptInlineEscaped: Html = {
    val before = """<script type="text/javascript" src="data:application/javascript,"""
    val after = """"></script>"""

    val sb = new JStringBuilder(before.length + asStringEscapedSize + after.length)

    sb.append(before)
    EscapeUtils.htmlAppendEscaped(sb, asString)
    sb.append(after)

    Html(sb.toString)
  }

  /** Create a script tag with this JS as the onload attribute.
    *
    * Example:
    * {{{
    *   <script type="text/javascript" src="//blah.js" onload="XX.m(&quot;hello&quot;)"></script>
    * }}}
    */
  def scriptOnLoad(url        : String,
                   async      : Boolean = false,
                   defer      : Boolean = false,
                   integrity  : String  = null,
                   crossorigin: String  = null,
                  ): Html = {

    var sbSize =
      tagStart.length +
      tagSrc.length +
      tagSrcLoad.length +
      tagEndEnd.length +
      url.length +
      asStringEscapedSize
    if (async) sbSize += tagAsync.length
    if (defer) sbSize += tagDefer.length
    if (integrity != null) sbSize += tagIntegrity1.length + tagIntegrity2.length + integrity.length
    if (crossorigin != null) sbSize += tagCrossorigin1.length + tagCrossorigin2.length + crossorigin.length

    val sb = new JStringBuilder(sbSize)

    sb.append(tagStart)
    if (async)
      sb.append(tagAsync)
    if (defer)
      sb.append(tagDefer)
    if (integrity != null) {
      sb.append(tagIntegrity1)
      sb.append(integrity)
      sb.append(tagIntegrity2)
    }
    if (crossorigin != null) {
      sb.append(tagCrossorigin1)
      sb.append(crossorigin)
      sb.append(tagCrossorigin2)
    }
    sb.append(tagSrc)
    sb.append(url)
    sb.append(tagSrcLoad)
    EscapeUtils.htmlAppendEscaped(sb, asString)
    sb.append(tagEndEnd)

    Html(sb.toString)
  }

  @inline private def asStringEscapedSize =
    // add an extra 10 + (50% of JS size) for HTML escaping
    asString.length + (asString.length >> 1) + 10
}

object Js {

  private[Js] object Const {
    final val tagStart        = "<script type=\"text/javascript\""
    final val tagAsync        = " async=\"async\""
    final val tagDefer        = " defer=\"defer\""
    final val tagIntegrity1   = " integrity=\""
    final val tagIntegrity2   = "\""
    final val tagCrossorigin1 = " crossorigin=\""
    final val tagCrossorigin2 = "\""
    final val tagSrc          = " src=\""
    final val tagSrcLoad      = "\" onload=\""
    final val tagEndEnd       = "\"></script>"
  }

  implicit def univEq: UnivEq[Js] =
    UnivEq.derive

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