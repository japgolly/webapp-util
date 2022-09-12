import sbt._

object GenJsonCodecs {

  def apply(srcRootDir: File): Unit = {

    val dir = srcRootDir / "japgolly/webapputil/circe"

    println()
    println("Generating JsonCodec boilerplate in: " + dir.getAbsolutePath)

    val merges = List.newBuilder[String]

    for (n <- 1 to 22) {
      val _As          = (1 to n).map('A' + _ - 1).map(_.toChar)
      val As           = _As.mkString(", ")
      val _as          = (1 to n).map('a' + _ - 1).map(_.toChar)
      val as           = _as.mkString(", ")
      val jJs          = _as.map(a => s"j$a: JsonCodec[${a.toUpper}]").mkString(", ")
      val js           = _as.map(a => s"j$a").mkString(", ")

      if (n > 1) {
        val encs = (1 to n).map { i =>
          val a = ('a' + i - 1).toChar
          s"      val $a = { val x = j$a.encoder(z._$i); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }"
        }.mkString("\n")

        val decs = _as.map(a =>
          s"        $a <- j$a.decoder(cur)"
        ).mkString("\n")

        merges +=
        s"""|  def merge$n[$As](implicit $jJs): JsonCodec[($As)] = {
            |    val enc = Encoder.instance[($As)] { z =>
            |      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
            |$encs
            |      ${_as.mkString("Json.fromJsonObject(", " deepMerge ", ")")}
            |    }
            |    val dec = Decoder.instance[($As)] { cur =>
            |      for {
            |$decs
            |      } yield ($as)
            |    }
            |    JsonCodec(enc, dec)
            |  }
            |""".stripMargin
      }
    }

    // =================================================================================================================

    def save(filename: String)(content: String): Unit = {
      println(s"Generating $filename ...")
      val c = content.trim + "\n"
//      println(c)
      IO.write(dir / filename, c)
    }

    save("JsonCodecArityBoilerplate.scala")(
      s"""package japgolly.webapputil.circe
         |
         |import io.circe._
         |
         |trait JsonCodecArityBoilerplate {
         |
         |${merges.result().mkString("\n")}
         |}
         |""".stripMargin
    )

    println()
  }
}
