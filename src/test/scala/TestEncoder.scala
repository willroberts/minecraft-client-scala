
import java.nio.{ByteBuffer, ByteOrder}

import org.junit.Test
import org.junit.Assert._

class TestSerializeMessage {
  @Test def testSerializeMessage(): Unit = {
    val expected: Array[Byte] = Array(
      14, 0, 0, 0, /** Request size, excluding these 4 bytes */
      1, 0, 0, 0, /** Request ID */
      MessageType.Command.id.toByte, 0, 0, 0, /** Message type */
      116, 101, 115, 116, /** Message body */
      0, 0 /** Terminator */
    )

    val result = MinecraftRCONClient.serializeMessage(1, MessageType.Command.id, "test")
    assertArrayEquals(result.array, expected)
  }
}

class TestDecodeResponse {
  @Test def testDecodeResponse(): Unit = {
    val expected: Response = Response(14, 1, MessageType.Response.id, "test")

    val input: ByteBuffer = ByteBuffer.allocate(16)
    input.order(ByteOrder.LITTLE_ENDIAN)
    input.putInt(1) /** Request ID (request size was already parsed before calling decodeResponse) */
    input.putInt(MessageType.Response.id) /** Message type */
    input.put("test".getBytes) /** Message body */
    input.flip()

    val result = MinecraftRCONClient.decodeResponse(input, 14, 1)
    assertEquals(result.get, expected)
  }
}