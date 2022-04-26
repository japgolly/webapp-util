package japgolly.webapputil.test

import japgolly.scalajs.react._
import japgolly.webapputil.webworker.AbstractWebWorker.TransferList
import japgolly.webapputil.webworker._
import scala.scalajs.js

object TestWebWorker {

  final class Client(server: () => Server) extends AbstractWebWorker.Client {

    private var onMessage: js.Any => Callback =
      _ => Callback.empty

    override def onError(f: OnError): Callback =
      Callback.empty

    override def listen(f: js.Any => Callback): Callback =
      Callback {
        onMessage = f
      }

    override def send(msg: js.Any, transferList: TransferList): Callback =
      Callback(server().recvFromClient(this, msg))

    def connect(s: Server): Unit =
      s.connect(this)

    def recvFromServer(msg: js.Any): Unit = {
      onMessage(msg).runNow()
    }
  }

  // ===================================================================================================================

  final class Server extends AbstractWebWorker.Server {
    override type Client = TestWebWorker.Client

    private var ports: List[Port] =
      Nil

    private var onConnect: Client => CallbackTo[js.Any => Callback] =
      _ => CallbackTo.pure(_ => Callback.empty)

    override def onError(f: OnError): Callback =
      Callback.empty

    override def listen(f: Client => CallbackTo[js.Any => Callback]): Callback =
      Callback {
        onConnect = f
      }

    override def send(to: IterableOnce[Client], msg: js.Any, transferList: TransferList): Callback =
      Callback {
        for (c <- to.iterator)
          c.recvFromServer(msg)
      }

    def newClient(connect: Boolean = true): Client = {
      val c = new Client(() => this)
      if (connect)
        this.connect(c)
      c
    }

    def connect(c: Client): Unit = {
      val port = new Port(c)
      ports ::= port
      port.onMessage = onConnect(c).runNow()
    }

    def recvFromClient(client: Client, msg: js.Any): Unit = {
      val port = ports.find(_.client eq client).getOrElse(sys error "Client not connected")
      port.onMessage(msg).runNow()
    }
  }

  final class Port(val client: Client) {
    var onMessage: js.Any => Callback =
      _ => Callback.empty
  }

  // ===================================================================================================================

  def pair(): (Client, Server) = {
    val s = new Server
    val c = s.newClient()
    (c, s)
  }
}
