package japgolly.webapputil.protocol.entrypoint

import japgolly.univeq.UnivEq
import japgolly.webapputil.protocol.general.EscapeUtil
import java.lang.{StringBuilder => JStringBuilder}

final case class Js(val asString: String) extends AnyVal {

  // def asXml = <script type="text/javascript">{asString(i)}</script>

  /** Create a script tag with this JS as the onload attribute. */
  def scriptOnLoad(url        : String,
                   async      : Boolean = false,
                   defer      : Boolean = false,
                   integrity  : String  = null,
                   crossorigin: String  = null,
                  ): Html = {
    import Js.Const._

    var sbSize =
      tagStart.length +
      tagSrc.length +
      tagSrcLoad.length +
      tagLoadEnd.length +
      url.length +
      asString.length +
      (asString.length >> 1) + 10 // add an extra 10 + (50% of JS size) for HTML escaping
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
    EscapeUtil.htmlAppendEscaped(sb, asString)
    sb.append(tagLoadEnd)

    Html(sb.toString)
  }
}

object Js {

  private[Js] object Const {
    final val tagStart        = "<script type=\"text/javascript\" "
    final val tagAsync        = "async=\"async\" "
    final val tagDefer        = "defer=\"defer\" "
    final val tagIntegrity1   = "integrity=\""
    final val tagIntegrity2   = "\" "
    final val tagCrossorigin1 = "crossorigin=\""
    final val tagCrossorigin2 = "\" "
    final val tagSrc          = "src=\""
    final val tagSrcLoad      = "\" onload=\""
    final val tagLoadEnd      = "\"></script>"
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