package japgolly.webapputil.protocol.entrypoint

import japgolly.microlibs.testutil.TestUtil._
import utest._

object EntrypointInvokerTest extends TestSuite {
  import EntrypointDef.Codec.ClearText._

  override def tests = Tests {

    "str" - {
      val d = EntrypointDef[String]("XX")
      val js = EntrypointInvoker(d)("hello")
      val onload = """onload="XX.m(&quot;hello&quot;)""""
      val script = """script type="text/javascript""""

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