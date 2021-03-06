package minecraft

import java.nio.{ByteBuffer, ByteOrder}

/** Message contains an RCON request or response. */
final case class Message (
        val Size: Int,
        val ID: Int,
        val Type: Int,
        val Body: String,
)

/** MessageType enumerates the type codes for RCON messages, e.g. 2 for 'COMMAND'. */
object MessageType extends Enumeration {
        type MessageType = Value
        val Response, Unused, Command, Authenticate = Value
}

/** MessageEncoder encapsulates message encoding and decoding logic. */
object MessageEncoder {
        val HeaderSize: Int = 10;
        val terminator: Array[Byte] = Array[Byte](0, 0)

        /** Encodes a message as little-endian bytes.
          * Format: [4-byte message size | 4-byte message id | 4-byte message size | variable length message | 2-byte terminator]. */
        def EncodeMessage(msg: Message): ByteBuffer = {
                val bytes: ByteBuffer = ByteBuffer.allocate(msg.Size+4)

                bytes.order(ByteOrder.LITTLE_ENDIAN)
                bytes.putInt(msg.Size)
                bytes.putInt(msg.ID)
                bytes.putInt(msg.Type)
                bytes.put(msg.Body.getBytes)
                bytes.put(terminator)
                bytes.flip

                bytes
        }

        /** Decodes a message from its little-endian byte representation.
          * Format: [4-byte message size | 4-byte message id | 4-byte message type | variable length message]. */
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

                Message(size, id, msgType, bodyBuffer.map(_.toChar).mkString)
        }
}