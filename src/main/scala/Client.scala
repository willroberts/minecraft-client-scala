package minecraft

import java.io.{InputStream, OutputStream}
import java.nio.{ByteBuffer, ByteOrder}
import java.util.concurrent.atomic.AtomicLong
import scala.util.{Try, Success, Failure}

/** RequestIDMismatchException is returned when the server responds with an unexpected response ID, indicating an error. */
final case class RequestIDMismatchException(
        private val message: String = "",
        private val cause: Throwable = None.orNull
) extends Exception(message, cause)

/** MinecraftRCONClient provides a TCP client for Minecraft RCON servers. */
object MinecraftRCONClient {
        /** Monotonic ID generator. */
        val requestID: AtomicLong = new AtomicLong(0)

        /** Authenticate logs into the RCON server */
        def Authenticate(password: String, writer: OutputStream, reader: InputStream): Try[Message] = {
                SendMessage(MessageType.Authenticate.id, password, writer, reader) match {
                        case Success(resp) => Success(resp)
                        case Failure(f) => Failure(f)
                }
        }

        /** SendMessage writes a serialized RCON message to the TCP socket and returns the response. */
        def SendMessage(msgType: Int, body: String, writer: OutputStream, reader: InputStream): Try[Message] = {
                val reqSize: Int = body.length + MessageEncoder.HeaderSize
                val reqID: Int = requestID.getAndIncrement.toInt
                val req: Message = Message(reqSize, reqID, msgType, body.getBytes)
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