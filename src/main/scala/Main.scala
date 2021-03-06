/** This is a Minecraft RCON client written in Scala, using Java stdlib networking.
  * Running this code connects to a server, authenticates, and retrieves the world seed. */
import java.io.{InputStream, OutputStream}
import java.net.{InetAddress, Socket}
import java.nio.{ByteBuffer, ByteOrder}
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicLong
import scala.io.StdIn.readLine
import scala.util.{Try, Success, Failure}
import scala.util.control.Breaks

/** Response contains the deseralized RCON response from the server. */
final case class Response (
        val Size: Int,
        val ID: Int,
        val MsgType: Int,
        val Body: String,
)

/** MessageType enumerates the type codes for RCON messages, e.g. 2 for 'COMMAND'. */
object MessageType extends Enumeration {
        type MessageType = Value
        val Response, Unused, Command, Authenticate = Value
}

/** RequestIDMismatchException is returned when the server responds with an unexpected response ID, indicating an error. */
final case class RequestIDMismatchException(
        private val message: String = "",
        private val cause: Throwable = None.orNull
) extends Exception(message, cause)

/** MinecraftRCONClient provides a TCP client for Minecraft RCON servers. */
object MinecraftRCONClient {
        /** Constants. */
        val Host: String = "127.0.0.1"
        val Port: Int = 25575
        val Password: String = "minecraft"
        val HeaderSize: Int = 10;

        /** Monotonic ID generator. */
        val requestID: AtomicLong = new AtomicLong(0)

        /** Main function runs an interactive shell for RCON commands. */
        @main def shell: Unit = {
                println("starting minecraft client")
                val sock: Socket = new Socket(InetAddress.getByName(Host), Port)
                val reader: InputStream = sock.getInputStream
                val writer: OutputStream = sock.getOutputStream

                println("authenticating")
                authenticate(Password, writer, reader) match {
                        case Success(_) => { println("authenticated successfully") }
                        case Failure(f) => {
                                println("failed to authenticate")
                                sock.close
                                return
                        }
                }

                println("starting RCON shell, quit with 'exit', 'quit', or Ctrl-C")
                val loop = new Breaks;
                loop.breakable {
                        while(true) {
                                print("> ")
                                val input = readLine()
                                if (Array("exit", "quit").contains(input)) {
                                        loop.break
                                }
                                sendMessage(MessageType.Command.id, input, writer, reader) match {
                                        case Success(resp) => { println(resp.Body) }
                                        case Failure(f) => { println("error for command '%s': %s".format(input, f.getMessage())) }
                                }
                        }
                }

                sock.close
        }

        /** authenticate logs into the RCON server */
        def authenticate(password: String, writer: OutputStream, reader: InputStream): Try[Response] = {
                sendMessage(MessageType.Authenticate.id, password, writer, reader) match {
                        case Success(resp) => Success(resp)
                        case Failure(f) => Failure(f)
                }
        }

        /** Writes a serialized RCON message to the TCP socket and returns the response. */
        def sendMessage(msgType: Int, msg: String, writer: OutputStream, reader: InputStream): Try[Response] = {
                val id: Int = requestID.getAndIncrement.toInt
                val bytes: ByteBuffer = serializeMessage(id, msgType, msg)

                /** Send the request to the server. */
                writer.write(bytes.array)
                writer.flush

                /** read the first four bytes of the response to determine how much data to read */
                val respSizeBuffer: ByteBuffer = ByteBuffer.wrap(reader.readNBytes(4))
                respSizeBuffer.order(ByteOrder.LITTLE_ENDIAN)
                val respSize: Int = respSizeBuffer.getInt

                /** read and decode all remaining data */
                val respBytesBuffer: ByteBuffer = ByteBuffer.wrap(reader.readNBytes(respSize))
                respBytesBuffer.order(ByteOrder.LITTLE_ENDIAN)
                decodeResponse(respBytesBuffer, respSize, id) match {
                        case Success(resp) => Success(resp)
                        case Failure(f) => Failure(f)
                }
        }

        /** Populate the request buffer.
          * Format: [4-byte request size | 4-byte request id | 4-byte message size | variable length message | 2-byte terminator]. */
        def serializeMessage(id: Int, msgType: Int, msg: String): ByteBuffer = {
                val size: Int = msg.getBytes.length + HeaderSize
                val bytes: ByteBuffer = ByteBuffer.allocate(size+4)

                bytes.order(ByteOrder.LITTLE_ENDIAN)
                bytes.putInt(size)
                bytes.putInt(id)
                bytes.putInt(msgType)
                bytes.put(msg.getBytes)
                bytes.put(0.toByte)
                bytes.put(0.toByte)
                bytes.flip

                bytes
        }

        /** Decode the remaining fields from the response buffer.
          * Format: [4-byte request id | 4-byte message type | variable length message]. */
        def decodeResponse(respBytes: ByteBuffer, respSize: Int, requestID: Int): Try[Response] = {
                val respID = respBytes.getInt()
                val msgType = respBytes.getInt()

                /** Detect errors by checking the response ID. */
                if (respID != requestID) {
                        Failure(new RequestIDMismatchException("invalid response ID: got %d, expected %d".format(respID, requestID)))
                }

                /** Read the response body if it exists. */
                var bodyString: String = ""
                if (respSize - HeaderSize > 0) {
                        var body: Array[Byte] = new Array[Byte](respSize-HeaderSize)
                        respBytes.get(body)
                        bodyString = (body.map(_.toChar)).mkString
                }

                Success(Response(respSize, respID, msgType, bodyString))
        }
}