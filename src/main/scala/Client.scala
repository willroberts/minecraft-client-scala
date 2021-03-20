package minecraft

import java.io.{InputStream, OutputStream}
import java.net.{InetAddress, Socket}
import java.nio.{ByteBuffer, ByteOrder}
import java.util.concurrent.atomic.AtomicLong
import scala.util.{Try, Success, Failure}

/** RequestIDMismatchException is returned when the server responds with an unexpected response ID, indicating an error. */
final case class RequestIDMismatchException(
        private val message: String = "",
        private val cause: Throwable = None.orNull
) extends Exception(message, cause)

/** MinecraftRCONClient provides a TCP client for Minecraft RCON servers. */
class MinecraftRCONClient(
        host: String,
        port: Int,
) {
        /** Connect on initialization */
        val sock: Socket = new Socket(InetAddress.getByName(host), port)
        val reader: InputStream = sock.getInputStream
        val writer: OutputStream = sock.getOutputStream

        /** Monotonic ID generator. */
        val requestID: AtomicLong = new AtomicLong(0)

        def Close() = {
                reader.close
                writer.close
                sock.close
        }

        /** Authenticate logs into the RCON server */
        def Authenticate(password: String): Try[Message] = {
                sendMessage(MessageType.Authenticate.id, password) match {
                        case Success(resp) => Success(resp)
                        case Failure(f) => Failure(f)
                }
        }

        /** SendCommand sends one RCON command to the server and returns the response. */
        def SendCommand(body: String): Try[Message] = {
                sendMessage(MessageType.Command.id, body)
        }

        /** sendMessage writes a serialized RCON message to the TCP socket and returns the response. */
        def sendMessage(msgType: Int, body: String): Try[Message] = {
                val reqSize: Int = body.length + MessageEncoder.HeaderSize
                val reqID: Int = requestID.getAndIncrement.toInt
                val req: Message = Message(reqSize, reqID, msgType, body)
                val reqBytes: ByteBuffer = MessageEncoder.EncodeMessage(req)

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
                val resp: Message = MessageEncoder.DecodeMessage(respBuffer)

                /** Detect errors by checking the response ID. */
                if (resp.ID != reqID) {
                        Failure(new RequestIDMismatchException("invalid response ID: got %d, expected %d".format(resp.ID, reqID)))
                } else {
                        Success(resp)
                }
        }
}