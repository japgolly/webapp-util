package japgolly.webapputil.circe

import io.circe._

trait JsonCodecArityBoilerplate {

  def merge2[A, B](implicit ja: JsonCodec[A], jb: JsonCodec[B]): JsonCodec[(A, B)] = {
    val enc = Encoder.instance[(A, B)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b)
    }
    val dec = Decoder.instance[(A, B)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
      } yield (a, b)
    }
    JsonCodec(enc, dec)
  }

  def merge3[A, B, C](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C]): JsonCodec[(A, B, C)] = {
    val enc = Encoder.instance[(A, B, C)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c)
    }
    val dec = Decoder.instance[(A, B, C)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
      } yield (a, b, c)
    }
    JsonCodec(enc, dec)
  }

  def merge4[A, B, C, D](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D]): JsonCodec[(A, B, C, D)] = {
    val enc = Encoder.instance[(A, B, C, D)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d)
    }
    val dec = Decoder.instance[(A, B, C, D)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
      } yield (a, b, c, d)
    }
    JsonCodec(enc, dec)
  }

  def merge5[A, B, C, D, E](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E]): JsonCodec[(A, B, C, D, E)] = {
    val enc = Encoder.instance[(A, B, C, D, E)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e)
    }
    val dec = Decoder.instance[(A, B, C, D, E)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
      } yield (a, b, c, d, e)
    }
    JsonCodec(enc, dec)
  }

  def merge6[A, B, C, D, E, F](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F]): JsonCodec[(A, B, C, D, E, F)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
      } yield (a, b, c, d, e, f)
    }
    JsonCodec(enc, dec)
  }

  def merge7[A, B, C, D, E, F, G](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G]): JsonCodec[(A, B, C, D, E, F, G)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
      } yield (a, b, c, d, e, f, g)
    }
    JsonCodec(enc, dec)
  }

  def merge8[A, B, C, D, E, F, G, H](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G], jh: JsonCodec[H]): JsonCodec[(A, B, C, D, E, F, G, H)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G, H)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val h = { val x = jh.encoder(z._8); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g deepMerge h)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G, H)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
        h <- jh.decoder(cur)
      } yield (a, b, c, d, e, f, g, h)
    }
    JsonCodec(enc, dec)
  }

  def merge9[A, B, C, D, E, F, G, H, I](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G], jh: JsonCodec[H], ji: JsonCodec[I]): JsonCodec[(A, B, C, D, E, F, G, H, I)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G, H, I)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val h = { val x = jh.encoder(z._8); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val i = { val x = ji.encoder(z._9); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g deepMerge h deepMerge i)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G, H, I)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
        h <- jh.decoder(cur)
        i <- ji.decoder(cur)
      } yield (a, b, c, d, e, f, g, h, i)
    }
    JsonCodec(enc, dec)
  }

  def merge10[A, B, C, D, E, F, G, H, I, J](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G], jh: JsonCodec[H], ji: JsonCodec[I], jj: JsonCodec[J]): JsonCodec[(A, B, C, D, E, F, G, H, I, J)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G, H, I, J)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val h = { val x = jh.encoder(z._8); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val i = { val x = ji.encoder(z._9); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val j = { val x = jj.encoder(z._10); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g deepMerge h deepMerge i deepMerge j)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G, H, I, J)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
        h <- jh.decoder(cur)
        i <- ji.decoder(cur)
        j <- jj.decoder(cur)
      } yield (a, b, c, d, e, f, g, h, i, j)
    }
    JsonCodec(enc, dec)
  }

  def merge11[A, B, C, D, E, F, G, H, I, J, K](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G], jh: JsonCodec[H], ji: JsonCodec[I], jj: JsonCodec[J], jk: JsonCodec[K]): JsonCodec[(A, B, C, D, E, F, G, H, I, J, K)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G, H, I, J, K)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val h = { val x = jh.encoder(z._8); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val i = { val x = ji.encoder(z._9); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val j = { val x = jj.encoder(z._10); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val k = { val x = jk.encoder(z._11); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g deepMerge h deepMerge i deepMerge j deepMerge k)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G, H, I, J, K)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
        h <- jh.decoder(cur)
        i <- ji.decoder(cur)
        j <- jj.decoder(cur)
        k <- jk.decoder(cur)
      } yield (a, b, c, d, e, f, g, h, i, j, k)
    }
    JsonCodec(enc, dec)
  }

  def merge12[A, B, C, D, E, F, G, H, I, J, K, L](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G], jh: JsonCodec[H], ji: JsonCodec[I], jj: JsonCodec[J], jk: JsonCodec[K], jl: JsonCodec[L]): JsonCodec[(A, B, C, D, E, F, G, H, I, J, K, L)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val h = { val x = jh.encoder(z._8); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val i = { val x = ji.encoder(z._9); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val j = { val x = jj.encoder(z._10); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val k = { val x = jk.encoder(z._11); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val l = { val x = jl.encoder(z._12); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g deepMerge h deepMerge i deepMerge j deepMerge k deepMerge l)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
        h <- jh.decoder(cur)
        i <- ji.decoder(cur)
        j <- jj.decoder(cur)
        k <- jk.decoder(cur)
        l <- jl.decoder(cur)
      } yield (a, b, c, d, e, f, g, h, i, j, k, l)
    }
    JsonCodec(enc, dec)
  }

  def merge13[A, B, C, D, E, F, G, H, I, J, K, L, M](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G], jh: JsonCodec[H], ji: JsonCodec[I], jj: JsonCodec[J], jk: JsonCodec[K], jl: JsonCodec[L], jm: JsonCodec[M]): JsonCodec[(A, B, C, D, E, F, G, H, I, J, K, L, M)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val h = { val x = jh.encoder(z._8); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val i = { val x = ji.encoder(z._9); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val j = { val x = jj.encoder(z._10); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val k = { val x = jk.encoder(z._11); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val l = { val x = jl.encoder(z._12); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val m = { val x = jm.encoder(z._13); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g deepMerge h deepMerge i deepMerge j deepMerge k deepMerge l deepMerge m)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
        h <- jh.decoder(cur)
        i <- ji.decoder(cur)
        j <- jj.decoder(cur)
        k <- jk.decoder(cur)
        l <- jl.decoder(cur)
        m <- jm.decoder(cur)
      } yield (a, b, c, d, e, f, g, h, i, j, k, l, m)
    }
    JsonCodec(enc, dec)
  }

  def merge14[A, B, C, D, E, F, G, H, I, J, K, L, M, N](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G], jh: JsonCodec[H], ji: JsonCodec[I], jj: JsonCodec[J], jk: JsonCodec[K], jl: JsonCodec[L], jm: JsonCodec[M], jn: JsonCodec[N]): JsonCodec[(A, B, C, D, E, F, G, H, I, J, K, L, M, N)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val h = { val x = jh.encoder(z._8); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val i = { val x = ji.encoder(z._9); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val j = { val x = jj.encoder(z._10); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val k = { val x = jk.encoder(z._11); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val l = { val x = jl.encoder(z._12); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val m = { val x = jm.encoder(z._13); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val n = { val x = jn.encoder(z._14); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g deepMerge h deepMerge i deepMerge j deepMerge k deepMerge l deepMerge m deepMerge n)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
        h <- jh.decoder(cur)
        i <- ji.decoder(cur)
        j <- jj.decoder(cur)
        k <- jk.decoder(cur)
        l <- jl.decoder(cur)
        m <- jm.decoder(cur)
        n <- jn.decoder(cur)
      } yield (a, b, c, d, e, f, g, h, i, j, k, l, m, n)
    }
    JsonCodec(enc, dec)
  }

  def merge15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G], jh: JsonCodec[H], ji: JsonCodec[I], jj: JsonCodec[J], jk: JsonCodec[K], jl: JsonCodec[L], jm: JsonCodec[M], jn: JsonCodec[N], jo: JsonCodec[O]): JsonCodec[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val h = { val x = jh.encoder(z._8); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val i = { val x = ji.encoder(z._9); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val j = { val x = jj.encoder(z._10); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val k = { val x = jk.encoder(z._11); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val l = { val x = jl.encoder(z._12); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val m = { val x = jm.encoder(z._13); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val n = { val x = jn.encoder(z._14); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val o = { val x = jo.encoder(z._15); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g deepMerge h deepMerge i deepMerge j deepMerge k deepMerge l deepMerge m deepMerge n deepMerge o)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
        h <- jh.decoder(cur)
        i <- ji.decoder(cur)
        j <- jj.decoder(cur)
        k <- jk.decoder(cur)
        l <- jl.decoder(cur)
        m <- jm.decoder(cur)
        n <- jn.decoder(cur)
        o <- jo.decoder(cur)
      } yield (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)
    }
    JsonCodec(enc, dec)
  }

  def merge16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G], jh: JsonCodec[H], ji: JsonCodec[I], jj: JsonCodec[J], jk: JsonCodec[K], jl: JsonCodec[L], jm: JsonCodec[M], jn: JsonCodec[N], jo: JsonCodec[O], jp: JsonCodec[P]): JsonCodec[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val h = { val x = jh.encoder(z._8); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val i = { val x = ji.encoder(z._9); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val j = { val x = jj.encoder(z._10); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val k = { val x = jk.encoder(z._11); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val l = { val x = jl.encoder(z._12); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val m = { val x = jm.encoder(z._13); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val n = { val x = jn.encoder(z._14); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val o = { val x = jo.encoder(z._15); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val p = { val x = jp.encoder(z._16); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g deepMerge h deepMerge i deepMerge j deepMerge k deepMerge l deepMerge m deepMerge n deepMerge o deepMerge p)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
        h <- jh.decoder(cur)
        i <- ji.decoder(cur)
        j <- jj.decoder(cur)
        k <- jk.decoder(cur)
        l <- jl.decoder(cur)
        m <- jm.decoder(cur)
        n <- jn.decoder(cur)
        o <- jo.decoder(cur)
        p <- jp.decoder(cur)
      } yield (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p)
    }
    JsonCodec(enc, dec)
  }

  def merge17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G], jh: JsonCodec[H], ji: JsonCodec[I], jj: JsonCodec[J], jk: JsonCodec[K], jl: JsonCodec[L], jm: JsonCodec[M], jn: JsonCodec[N], jo: JsonCodec[O], jp: JsonCodec[P], jq: JsonCodec[Q]): JsonCodec[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val h = { val x = jh.encoder(z._8); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val i = { val x = ji.encoder(z._9); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val j = { val x = jj.encoder(z._10); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val k = { val x = jk.encoder(z._11); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val l = { val x = jl.encoder(z._12); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val m = { val x = jm.encoder(z._13); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val n = { val x = jn.encoder(z._14); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val o = { val x = jo.encoder(z._15); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val p = { val x = jp.encoder(z._16); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val q = { val x = jq.encoder(z._17); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g deepMerge h deepMerge i deepMerge j deepMerge k deepMerge l deepMerge m deepMerge n deepMerge o deepMerge p deepMerge q)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
        h <- jh.decoder(cur)
        i <- ji.decoder(cur)
        j <- jj.decoder(cur)
        k <- jk.decoder(cur)
        l <- jl.decoder(cur)
        m <- jm.decoder(cur)
        n <- jn.decoder(cur)
        o <- jo.decoder(cur)
        p <- jp.decoder(cur)
        q <- jq.decoder(cur)
      } yield (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q)
    }
    JsonCodec(enc, dec)
  }

  def merge18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G], jh: JsonCodec[H], ji: JsonCodec[I], jj: JsonCodec[J], jk: JsonCodec[K], jl: JsonCodec[L], jm: JsonCodec[M], jn: JsonCodec[N], jo: JsonCodec[O], jp: JsonCodec[P], jq: JsonCodec[Q], jr: JsonCodec[R]): JsonCodec[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val h = { val x = jh.encoder(z._8); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val i = { val x = ji.encoder(z._9); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val j = { val x = jj.encoder(z._10); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val k = { val x = jk.encoder(z._11); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val l = { val x = jl.encoder(z._12); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val m = { val x = jm.encoder(z._13); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val n = { val x = jn.encoder(z._14); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val o = { val x = jo.encoder(z._15); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val p = { val x = jp.encoder(z._16); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val q = { val x = jq.encoder(z._17); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val r = { val x = jr.encoder(z._18); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g deepMerge h deepMerge i deepMerge j deepMerge k deepMerge l deepMerge m deepMerge n deepMerge o deepMerge p deepMerge q deepMerge r)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
        h <- jh.decoder(cur)
        i <- ji.decoder(cur)
        j <- jj.decoder(cur)
        k <- jk.decoder(cur)
        l <- jl.decoder(cur)
        m <- jm.decoder(cur)
        n <- jn.decoder(cur)
        o <- jo.decoder(cur)
        p <- jp.decoder(cur)
        q <- jq.decoder(cur)
        r <- jr.decoder(cur)
      } yield (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r)
    }
    JsonCodec(enc, dec)
  }

  def merge19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G], jh: JsonCodec[H], ji: JsonCodec[I], jj: JsonCodec[J], jk: JsonCodec[K], jl: JsonCodec[L], jm: JsonCodec[M], jn: JsonCodec[N], jo: JsonCodec[O], jp: JsonCodec[P], jq: JsonCodec[Q], jr: JsonCodec[R], js: JsonCodec[S]): JsonCodec[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val h = { val x = jh.encoder(z._8); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val i = { val x = ji.encoder(z._9); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val j = { val x = jj.encoder(z._10); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val k = { val x = jk.encoder(z._11); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val l = { val x = jl.encoder(z._12); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val m = { val x = jm.encoder(z._13); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val n = { val x = jn.encoder(z._14); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val o = { val x = jo.encoder(z._15); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val p = { val x = jp.encoder(z._16); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val q = { val x = jq.encoder(z._17); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val r = { val x = jr.encoder(z._18); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val s = { val x = js.encoder(z._19); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g deepMerge h deepMerge i deepMerge j deepMerge k deepMerge l deepMerge m deepMerge n deepMerge o deepMerge p deepMerge q deepMerge r deepMerge s)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
        h <- jh.decoder(cur)
        i <- ji.decoder(cur)
        j <- jj.decoder(cur)
        k <- jk.decoder(cur)
        l <- jl.decoder(cur)
        m <- jm.decoder(cur)
        n <- jn.decoder(cur)
        o <- jo.decoder(cur)
        p <- jp.decoder(cur)
        q <- jq.decoder(cur)
        r <- jr.decoder(cur)
        s <- js.decoder(cur)
      } yield (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s)
    }
    JsonCodec(enc, dec)
  }

  def merge20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G], jh: JsonCodec[H], ji: JsonCodec[I], jj: JsonCodec[J], jk: JsonCodec[K], jl: JsonCodec[L], jm: JsonCodec[M], jn: JsonCodec[N], jo: JsonCodec[O], jp: JsonCodec[P], jq: JsonCodec[Q], jr: JsonCodec[R], js: JsonCodec[S], jt: JsonCodec[T]): JsonCodec[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val h = { val x = jh.encoder(z._8); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val i = { val x = ji.encoder(z._9); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val j = { val x = jj.encoder(z._10); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val k = { val x = jk.encoder(z._11); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val l = { val x = jl.encoder(z._12); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val m = { val x = jm.encoder(z._13); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val n = { val x = jn.encoder(z._14); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val o = { val x = jo.encoder(z._15); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val p = { val x = jp.encoder(z._16); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val q = { val x = jq.encoder(z._17); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val r = { val x = jr.encoder(z._18); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val s = { val x = js.encoder(z._19); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val t = { val x = jt.encoder(z._20); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g deepMerge h deepMerge i deepMerge j deepMerge k deepMerge l deepMerge m deepMerge n deepMerge o deepMerge p deepMerge q deepMerge r deepMerge s deepMerge t)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
        h <- jh.decoder(cur)
        i <- ji.decoder(cur)
        j <- jj.decoder(cur)
        k <- jk.decoder(cur)
        l <- jl.decoder(cur)
        m <- jm.decoder(cur)
        n <- jn.decoder(cur)
        o <- jo.decoder(cur)
        p <- jp.decoder(cur)
        q <- jq.decoder(cur)
        r <- jr.decoder(cur)
        s <- js.decoder(cur)
        t <- jt.decoder(cur)
      } yield (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t)
    }
    JsonCodec(enc, dec)
  }

  def merge21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G], jh: JsonCodec[H], ji: JsonCodec[I], jj: JsonCodec[J], jk: JsonCodec[K], jl: JsonCodec[L], jm: JsonCodec[M], jn: JsonCodec[N], jo: JsonCodec[O], jp: JsonCodec[P], jq: JsonCodec[Q], jr: JsonCodec[R], js: JsonCodec[S], jt: JsonCodec[T], ju: JsonCodec[U]): JsonCodec[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val h = { val x = jh.encoder(z._8); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val i = { val x = ji.encoder(z._9); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val j = { val x = jj.encoder(z._10); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val k = { val x = jk.encoder(z._11); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val l = { val x = jl.encoder(z._12); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val m = { val x = jm.encoder(z._13); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val n = { val x = jn.encoder(z._14); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val o = { val x = jo.encoder(z._15); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val p = { val x = jp.encoder(z._16); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val q = { val x = jq.encoder(z._17); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val r = { val x = jr.encoder(z._18); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val s = { val x = js.encoder(z._19); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val t = { val x = jt.encoder(z._20); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val u = { val x = ju.encoder(z._21); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g deepMerge h deepMerge i deepMerge j deepMerge k deepMerge l deepMerge m deepMerge n deepMerge o deepMerge p deepMerge q deepMerge r deepMerge s deepMerge t deepMerge u)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
        h <- jh.decoder(cur)
        i <- ji.decoder(cur)
        j <- jj.decoder(cur)
        k <- jk.decoder(cur)
        l <- jl.decoder(cur)
        m <- jm.decoder(cur)
        n <- jn.decoder(cur)
        o <- jo.decoder(cur)
        p <- jp.decoder(cur)
        q <- jq.decoder(cur)
        r <- jr.decoder(cur)
        s <- js.decoder(cur)
        t <- jt.decoder(cur)
        u <- ju.decoder(cur)
      } yield (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u)
    }
    JsonCodec(enc, dec)
  }

  def merge22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V](implicit ja: JsonCodec[A], jb: JsonCodec[B], jc: JsonCodec[C], jd: JsonCodec[D], je: JsonCodec[E], jf: JsonCodec[F], jg: JsonCodec[G], jh: JsonCodec[H], ji: JsonCodec[I], jj: JsonCodec[J], jk: JsonCodec[K], jl: JsonCodec[L], jm: JsonCodec[M], jn: JsonCodec[N], jo: JsonCodec[O], jp: JsonCodec[P], jq: JsonCodec[Q], jr: JsonCodec[R], js: JsonCodec[S], jt: JsonCodec[T], ju: JsonCodec[U], jv: JsonCodec[V]): JsonCodec[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V)] = {
    val enc = Encoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V)] { z =>
      def fail(z: Json): Nothing = throw new IllegalStateException("Expected a JsonObject, got: " + z.noSpaces)
      val a = { val x = ja.encoder(z._1); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val b = { val x = jb.encoder(z._2); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val c = { val x = jc.encoder(z._3); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val d = { val x = jd.encoder(z._4); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val e = { val x = je.encoder(z._5); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val f = { val x = jf.encoder(z._6); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val g = { val x = jg.encoder(z._7); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val h = { val x = jh.encoder(z._8); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val i = { val x = ji.encoder(z._9); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val j = { val x = jj.encoder(z._10); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val k = { val x = jk.encoder(z._11); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val l = { val x = jl.encoder(z._12); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val m = { val x = jm.encoder(z._13); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val n = { val x = jn.encoder(z._14); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val o = { val x = jo.encoder(z._15); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val p = { val x = jp.encoder(z._16); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val q = { val x = jq.encoder(z._17); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val r = { val x = jr.encoder(z._18); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val s = { val x = js.encoder(z._19); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val t = { val x = jt.encoder(z._20); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val u = { val x = ju.encoder(z._21); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      val v = { val x = jv.encoder(z._22); val oo = x.asObject; if (oo.isEmpty) fail(x); oo.get }
      Json.fromJsonObject(a deepMerge b deepMerge c deepMerge d deepMerge e deepMerge f deepMerge g deepMerge h deepMerge i deepMerge j deepMerge k deepMerge l deepMerge m deepMerge n deepMerge o deepMerge p deepMerge q deepMerge r deepMerge s deepMerge t deepMerge u deepMerge v)
    }
    val dec = Decoder.instance[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V)] { cur =>
      for {
        a <- ja.decoder(cur)
        b <- jb.decoder(cur)
        c <- jc.decoder(cur)
        d <- jd.decoder(cur)
        e <- je.decoder(cur)
        f <- jf.decoder(cur)
        g <- jg.decoder(cur)
        h <- jh.decoder(cur)
        i <- ji.decoder(cur)
        j <- jj.decoder(cur)
        k <- jk.decoder(cur)
        l <- jl.decoder(cur)
        m <- jm.decoder(cur)
        n <- jn.decoder(cur)
        o <- jo.decoder(cur)
        p <- jp.decoder(cur)
        q <- jq.decoder(cur)
        r <- jr.decoder(cur)
        s <- js.decoder(cur)
        t <- jt.decoder(cur)
        u <- ju.decoder(cur)
        v <- jv.decoder(cur)
      } yield (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v)
    }
    JsonCodec(enc, dec)
  }

}
