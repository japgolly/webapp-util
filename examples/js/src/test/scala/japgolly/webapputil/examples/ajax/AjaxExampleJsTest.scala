package japgolly.webapputil.examples.ajax

import japgolly.microlibs.testutil.TestUtil._
import japgolly.scalajs.react.AsyncCallback
import japgolly.scalajs.react.test._
import japgolly.webapputil.ajax.AjaxClient
import japgolly.webapputil.circe.test.TestJsonAjaxClient
import japgolly.webapputil.general.{ErrorMsg, ServerSideProcInvoker}
import org.scalajs.dom.HTMLButtonElement
import utest._

object AjaxExampleJsTest extends TestSuite {

  override def tests = Tests {
    "component1" - {
      "success" - testExampleComponent1Success()
      "failure" - testExampleComponent1Failure()
    }
    "component2" - testExampleComponent2()
  }

  // ===================================================================================================================
  private def testExampleComponent1Success() = {
    import AjaxExampleJs.ExampleComponent._

    // Here we provide our own ServerSideProcInvoker that provides an answer in-memory immediately
    val props = Props(ServerSideProcInvoker { req =>
      val result = AjaxExampleShared.AddInts.logic(req)
      AsyncCallback.pure(Right(result))
    })

    // Mount the component for testing...
    ReactTestUtils.withRenderedIntoBody(Component(props)) { m =>
      def dom() = m.getDOMNode.asMounted().asHtml()

      // Test initial state
      assertContains(dom().innerHTML, "What really is 2 + 2?")

      // Test clicking through to the immediate test result
      Simulate.click(dom())
      assertContains(dom().innerHTML, "2 + 2 = 4")
    }
  }

  // ===================================================================================================================
  private def testExampleComponent1Failure() = {
    import AjaxExampleJs.ExampleComponent._

    // Let's simulate a connectivity error
    val error = ErrorMsg("No internet connectivity")
    val props = Props(ServerSideProcInvoker.const(Left(error)))

    // Mount the component for testing...
    ReactTestUtils.withRenderedIntoBody(Component(props)) { m =>
      def dom() = m.getDOMNode.asMounted().asHtml()

      // Click button, and confirm the component gracefully handles the error
      Simulate.click(dom())
      assertContains(dom().innerHTML, "Error: No internet connectivity")
    }
  }

  // ===================================================================================================================
  private def testExampleComponent2() = {
    import AjaxExampleJs.ExampleComponent2._

    // Here we create our own in-memory client for lower-level ajax testing.
    //
    // We're setting autoRespondInitially to false so that when it receives a request, it does nothing and waits for us
    // to command it to respond. This will help us test the state of the component when a request is in-flight.
    //
    val ajax = TestJsonAjaxClient(autoRespondInitially = false)

    // Here we teach our ajax client how to respond to calls to /add-ints
    ajax.addAutoResponse(AjaxExampleShared.AddInts.protocol) { testReq =>
      val result = AjaxExampleShared.AddInts.logic(testReq.input)
      val response = AjaxClient.Response.success(result)
      testReq.onResponse(Right(response))
    }

    // Mount the component for testing...
    ReactTestUtils.withRenderedIntoBody(Component(Props(ajax))) { m =>
      def dom() = m.getDOMNode.asMounted().asHtml()

      // Test initial state
      assertContains(dom().innerHTML, "What really is 2 + 2?")

      // Click button, and test that the button is disabled, pending the ajax result
      Simulate.click(dom())
      assert(dom().asInstanceOf[HTMLButtonElement].disabled)

      // Release the ajax response, and test the component displays it
      ajax.autoRespondToLast()
      assertContains(dom().innerHTML, "2 + 2 = 4")
    }
  }
}