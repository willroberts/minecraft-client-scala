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

/** Message contains an RCON request or response. */
final case class Message (
        val Size: Int,
        val ID: Int,
        val Type: Int,
        val Body: Array[Byte],
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
        val DefaultHost: String = "127.0.0.1"
        val DefaultPort: Int = 25575
        val DefaultPassword: String = "minecraft"
        val HeaderSize: Int = 10;

        /** Monotonic ID generator. */
        val requestID: AtomicLong = new AtomicLong(0)

        /** Main function runs an interactive shell for RCON commands. */
        @main def shell(args: String*): Unit = {
                var host: String = DefaultHost
                var port: Int = DefaultPort
                var password: String = DefaultPassword

                /** Parse command-line arguments if provided */
                for ( i <- 0 until args.length ) {
                        args(i) match {
                                case "--host" => { host = args(i+1) }
                                case "--port" => { port = args(i+1).toInt }
                                case "--password" => { password = args(i+1) }
                                case _ => { }
                        }
                }

                /** Connect to the server and authenticate */
                val sock: Socket = new Socket(InetAddress.getByName(host), port)
                val reader: InputStream = sock.getInputStream
                val writer: OutputStream = sock.getOutputStream
                authenticate(password, writer, reader) match {
                        case Success(_) => {}
                        case Failure(f) => {
                                println("failed to authenticate")
                                sock.close
                                System.exit(1)
                        }
                }

                /** Start the RCON shell */
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
                                        case Success(resp) => { println((resp.Body.map(_.toChar)).mkString) }
                                        case Failure(f) => { println("error for command '%s': %s".format(input, f.getMessage())) }
                                }
                        }
                }
                sock.close
                System.exit(0)
        }

        /** authenticate logs into the RCON server */
        def authenticate(password: String, writer: OutputStream, reader: InputStream): Try[Message] = {
                sendMessage(MessageType.Authenticate.id, password, writer, reader) match {
                        case Success(resp) => Success(resp)
                        case Failure(f) => Failure(f)
                }
        }

        /** Writes a serialized RCON message to the TCP socket and returns the response. */
        def sendMessage(msgType: Int, body: String, writer: OutputStream, reader: InputStream): Try[Message] = {
                val reqSize: Int = body.length + HeaderSize
                val reqID: Int = requestID.getAndIncrement.toInt
                val req: Message = Message(reqSize, reqID, msgType, body.getBytes)
                val reqBytes: ByteBuffer = EncodeMessage(req)

                /** Send the request to the server. */
                writer.write(reqBytes.array)
                writer.flush

                /** Read and parse the response. */
                /** Detect the total size to read */
                var sizeBytes: Array[Byte] = reader.readNBytes(4)
                val sizeBuffer: ByteBuffer = ByteBuffer.wrap(sizeBytes)
                sizeBuffer.order(ByteOrder.LITTLE_ENDIAN)
                val respSize: Int = sizeBuffer.getInt
                sizeBuffer.flip

                /** Read remaining bytes */
                val respBuffer: ByteBuffer = ByteBuffer.allocate(respSize+4).put(sizeBuffer)
                respBuffer.order(ByteOrder.LITTLE_ENDIAN)
                respBuffer.put(reader.readNBytes(respSize))
                respBuffer.flip
                val resp: Message = DecodeMessage(respBuffer)

                /** Detect errors by checking the response ID. */
                if (resp.ID != reqID) {
                        Failure(new RequestIDMismatchException("invalid response ID: got %d, expected %d".format(resp.ID, reqID)))
                } else {
                        Success(resp)
                }
        }

        /** Populate the request buffer.
          * Format: [4-byte request size | 4-byte request id | 4-byte message size | variable length message | 2-byte terminator]. */
        def EncodeMessage(msg: Message): ByteBuffer = {
                val bytes: ByteBuffer = ByteBuffer.allocate(msg.Size+4)

                bytes.order(ByteOrder.LITTLE_ENDIAN)
                bytes.putInt(msg.Size)
                bytes.putInt(msg.ID)
                bytes.putInt(msg.Type)
                bytes.put(msg.Body)
                bytes.put(0.toByte)
                bytes.put(0.toByte)
                bytes.flip

                bytes
        }

        /** Decode the remaining fields from the response buffer.
          * Format: [4-byte request id | 4-byte message type | variable length message]. */
        def DecodeMessage(bytes: ByteBuffer): Message = {
                bytes.order(ByteOrder.LITTLE_ENDIAN)
                val size: Int = bytes.getInt
                val id: Int = bytes.getInt
                val msgType: Int = bytes.getInt

                /** Read the response body if it exists. */
                val remainingBytes: Int = size - HeaderSize
                var bodyBuffer: Array[Byte] = new Array[Byte](remainingBytes)
                for (i <- 0 until remainingBytes) {
                        bodyBuffer(i) = bytes.get
                }

                Message(size, id, msgType, bodyBuffer)
        }
}