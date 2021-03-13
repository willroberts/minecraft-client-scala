
import java.nio.{ByteBuffer, ByteOrder}

import org.junit.Test
import org.junit.Assert._

class TestEncodeMessage {
  @Test def testEncodeMessage(): Unit = {
    val expected: Array[Byte] = Array(
      14, 0, 0, 0, /** Message size, excluding these 4 bytes */
      1, 0, 0, 0, /** Message ID */
      MessageType.Command.id.toByte, 0, 0, 0, /** Message type */
      116, 101, 115, 116, /** Message body */
      0, 0 /** Terminator */
    )

    val result = MinecraftRCONClient.EncodeMessage(Message(14, 1, MessageType.Command.id, "test".getBytes))
    assertArrayEquals(result.array, expected)
  }
}

class TestDecodeMessage {
  @Test def testDecodeMessage(): Unit = {
    val expected: Message = Message(14, 1, MessageType.Response.id, "test".getBytes)

    val input: ByteBuffer = ByteBuffer.allocate(16)
    input.order(ByteOrder.LITTLE_ENDIAN)
    input.putInt(14) /** Message size */
    input.putInt(1) /** Message ID */
    input.putInt(MessageType.Response.id) /** Message type */
    input.put("test".getBytes) /** Message body */
    input.flip()

    val output = MinecraftRCONClient.DecodeMessage(input)
    assertEquals(output.Size, expected.Size)
    assertEquals(output.ID, expected.ID)
    assertEquals(output.Type, expected.Type)
    assertArrayEquals(output.Body, expected.Body)
  }
}