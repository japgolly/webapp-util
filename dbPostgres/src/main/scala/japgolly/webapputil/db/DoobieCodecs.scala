package japgolly.webapputil.db

import doobie._
import doobie.postgres.implicits._
import java.time._
import org.postgresql.util.PGInterval

object DoobieCodecs extends DoobieCodecs

trait DoobieCodecs {

  protected final val UTC = ZoneId.of("UTC")

  implicit val doobieWriteDuration: Write[Duration] =
    Write[PGInterval].contramap(d =>
      new PGInterval(
        0, 0, 0, 0, 0, // years, months, days, hours, minutes
        d.getSeconds.toDouble + d.getNano / 1000000000.0,
      ))

  implicit val doobieMetaInstant: Meta[Instant] =
    Meta[OffsetDateTime].timap(_.toInstant)(OffsetDateTime.ofInstant(_, UTC))
}
