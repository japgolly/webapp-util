package japgolly.webapputil.examples.entrypoint

import boopickle._
import japgolly.microlibs.testutil.TestUtil._
import japgolly.univeq.UnivEq
import japgolly.webapputil.binary.BinaryData
import utest._

object BackendTest extends TestSuite {
  import EntrypointExample.InitialData

  // Declare that == is fine for testing equality of InitialData instances
  private implicit def univEqInitialData: UnivEq[InitialData] = UnivEq.derive

  // Sample data
  private val initData = InitialData(username = "someone123")

  // The base64 encoding of `initData` after binary serialisation
  private val initData64 = "CnNvbWVvbmUxMjM="

  // Helper to parse BinaryData into InitialData
  private def deserialiseInitialData(b: BinaryData): InitialData =
    UnpickleImpl(EntrypointExample.picklerInitialData).fromBytes(b.unsafeByteBuffer)

  override def tests = Tests {

    // =================================================================================
    // Verify the initData64 deserialises to initData
    "pickler" - assertEq(
      deserialiseInitialData(BinaryData.fromBase64(initData64)),
      initData)

    // =================================================================================
    // Let's start with our Backend.generateHtml() method
    "generateHtml" - {

      // Verify the total HTML output
      val js64 = "TXlFeGFtcGxlQXBwLm0oIkNuTnZiV1Z2Ym1VeE1qTT0iKQ=="
      assertEq(
        Backend.generateHtml(initData).asString,
        s"""<script type="text/javascript" src="data:application/javascript;base64,$js64"></script>""")

      // Verify the JS after base64 decoding
      assertEq(
        BinaryData.fromBase64(js64).toStringAsUtf8,
        s"""MyExampleApp.m("$initData64")""")
    }

    // =================================================================================
    // As above, but the RunOnWindowLoad variant
    "generateHtmlToRunOnWindowLoad" - {

      // Verify the total HTML output
      val js64 = "d2luZG93Lm9ubG9hZD1mdW5jdGlvbigpe015RXhhbXBsZUFwcC5tKCJDbk52YldWdmJtVXhNak09Iil9Ow=="
      assertEq(
        Backend.generateHtmlToRunOnWindowLoad(initData).asString,
        s"""<script type="text/javascript" src="data:application/javascript;base64,$js64"></script>""")

      // Verify the JS after base64 decoding
      assertEq(
        BinaryData.fromBase64(js64).toStringAsUtf8,
        s"""window.onload=function(){MyExampleApp.m("$initData64")};""")
    }
  }
}
