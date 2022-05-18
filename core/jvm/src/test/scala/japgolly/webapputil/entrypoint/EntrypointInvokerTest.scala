package japgolly.webapputil.entrypoint

import japgolly.microlibs.testutil.TestUtil._
import japgolly.webapputil.binary.BinaryData
import utest._

object EntrypointInvokerTest extends TestSuite {
  import EntrypointDef.Codec.ClearText._

  private val script = """script type="text/javascript""""
  private val d = EntrypointDef[String]("XX")

  override def tests = Tests {

    "scriptInline" - {
      val js = EntrypointInvoker(d)("console.log('</script>')")

      "base64" - {
        val b64 = "WFgubSgiY29uc29sZS5sb2coJzwvc2NyaXB0PicpIik="

        assertEq(
          js.scriptInlineBase64.asString,
          s"""<script type="text/javascript" src="data:application/javascript;base64,$b64"></script>""")

        assertEq(
          BinaryData.fromBase64(b64).toStringAsUtf8,
          """XX.m("console.log('</script>')")""")
      }

      "escaped" - assertEq(
        js.scriptInlineEscaped.asString,
        s"""<$script src="data:application/javascript,XX.m(&quot;console.log(&#39;&lt;/script&gt;&#39;)&quot;)"></script>""")
    }

    "scriptOnLoad" - {
      val js = EntrypointInvoker(d)("hello")
      val onload = """onload="XX.m(&quot;hello&quot;)""""

      "basic" - assertEq(
        js.scriptOnLoad("//blah.js").asString,
        s"""<$script src="//blah.js" $onload></script>""")

      "async" - assertEq(
        js.scriptOnLoad("//blah.js", async = true).asString,
        s"""<$script async="async" src="//blah.js" $onload></script>""")

      "defer" - assertEq(
        js.scriptOnLoad("//blah.js", defer = true).asString,
        s"""<$script defer="defer" src="//blah.js" $onload></script>""")

      "integrity" - assertEq(
        js.scriptOnLoad("//blah.js", integrity = "123").asString,
        s"""<$script integrity="123" src="//blah.js" $onload></script>""")

      "crossorigin" - assertEq(
        js.scriptOnLoad("//blah.js", crossorigin = "abc").asString,
        s"""<$script crossorigin="abc" src="//blah.js" $onload></script>""")
    }

  }
}