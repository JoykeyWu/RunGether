package com.rungether.app.bluetooth.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 蓝牙指令 JSON 编解码单元测试
 *
 * 校验双端共用的协议骨架可以无损往返，并对非法输入安全降级为 null。
 */
class CommandCodecTest {

    // 方向指令：编码后再解码应得到相同对象
    @Test
    fun direction_roundTrip_keepsTypeAndAngle() {
        val original = GuideCommand.Direction(DirectionType.TURN, 45)
        val encoded = CommandCodec.encode(original)
        assertTrue("编码应以换行结尾", encoded.endsWith("\n"))
        val decoded = CommandCodec.decode(encoded.trim())
        assertEquals(original, decoded)
    }

    // 快捷指令：障碍类型应可往返
    @Test
    fun shortcut_obstacle_roundTrips() {
        val original = GuideCommand.Shortcut(ShortcutType.OBSTACLE)
        val decoded = CommandCodec.decode(CommandCodec.encode(original).trim())
        assertEquals(original, decoded)
    }

    // SOS 指令：编码再解码应保持单例语义
    @Test
    fun sos_roundTrips() {
        val encoded = CommandCodec.encode(GuideCommand.Sos)
        val decoded = CommandCodec.decode(encoded.trim())
        assertNotNull(decoded)
        assertTrue(decoded is GuideCommand.Sos)
    }

    // 非法 JSON 应返回 null 而不是抛异常
    @Test
    fun decode_invalidJson_returnsNull() {
        assertNull(CommandCodec.decode("not-a-json"))
        assertNull(CommandCodec.decode("{\"kind\":\"unknown\"}"))
        assertNull(CommandCodec.decode(""))
    }
}
