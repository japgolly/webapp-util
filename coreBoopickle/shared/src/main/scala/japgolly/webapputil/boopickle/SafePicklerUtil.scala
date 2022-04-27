package japgolly.webapputil.boopickle

import boopickle.{PickleState, UnpickleState}
import japgolly.webapputil.general.Version

object SafePicklerUtil {
  import PicklerUtil._

  final case class UnsupportedVersionException(found: Version, maxSupported: Version)
    extends RuntimeException(s"${found.verStr} not supported. ${maxSupported.verStr} is the max supported.")

  case object CorruptData
    extends RuntimeException("Corrupt data.")

  /** Used to add a codec version to a binary protocol whilst retaining backwards-compatibility with the unversioned
  * case.
  */
  final val VersionHeader = -99988999

  def writeVersion(ver: Int)(implicit state: PickleState): Unit = {
    assert(ver > 0) // v1.0 is the default and doesn't need a version header
    state.enc.writeInt(VersionHeader)
    state.enc.writeInt(ver)
  }

  def unsupportedVer(ver: Int, maxSupportedVer: Int): Nothing =
    throw UnsupportedVersionException(found = Version.v1(ver), maxSupported = Version.v1(maxSupportedVer))

  def readByVersion[A](maxSupportedVer: Int)(f: PartialFunction[Int, A])(implicit state: UnpickleState): A = {
    assert(maxSupportedVer > 0)

    def unsupportedVer(ver: Int): Nothing =
      SafePicklerUtil.unsupportedVer(ver, maxSupportedVer)

    def readVer(ver: Int): A =
      f.applyOrElse[Int, A](ver, unsupportedVer)

    state.dec.peek(_.readInt) match {
      case VersionHeader =>
        state.dec.readInt
        val ver = state.dec.readInt
        if (ver <= 0)
          throw CorruptData
        if (ver > maxSupportedVer) // preempt using the partial function in case maxSupportedVer is incorrect
          unsupportedVer(ver)
        readVer(ver)
      case _ =>
        readVer(0)
    }
  }

}
