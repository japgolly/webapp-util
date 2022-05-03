package japgolly.webapputil.general

import japgolly.microlibs.testutil.TestUtil._
import japgolly.webapputil.webstorage.AbstractWebStorage
import utest._

object VarJsTest extends TestSuite {

  override def tests = Tests {

    "var" - {
      val v = VarJs(1)
      assertEq(v.unsafeGet(), 1)
      v.unsafeSet(3)
      assertEq(v.unsafeGet(), 3)
    }

    "webStorage" - {
      "boolean" - {
        val s = AbstractWebStorage.inMemory()
        val k = AbstractWebStorage.Key("blah")
        val v1 = VarJs.webStorage.boolean(s, k)
        val v2 = VarJs.webStorage.boolean(s, k)
        def get() = (v1.unsafeGet(), v2.unsafeGet())
        assertEq(get(), (false, false))
        v1.unsafeSet(true)
        assertEq(get(), (true, true))
        v2.unsafeSet(false)
        assertEq(get(), (false, false))
      }
    }
  }
}
