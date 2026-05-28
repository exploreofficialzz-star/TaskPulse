package com.chastechgroup.taskpulse

import com.chastechgroup.taskpulse.data.models.CommandAction
import com.chastechgroup.taskpulse.data.models.CommandTrigger
import com.chastechgroup.taskpulse.data.models.FocusMode
import com.chastechgroup.taskpulse.engine.CommandParser
import org.junit.Assert.*
import org.junit.Test

class CommandParserTest {

    @Test fun `block instagram`() {
        val cmd = CommandParser.parse("Block Instagram for 1 hour")
        assertEquals(CommandAction.BLOCK_APP, cmd.action)
        assertTrue(cmd.targetApps.contains("com.instagram.android"))
        assertEquals(3600L, cmd.durationSeconds)
    }

    @Test fun `alternative block phrasing`() {
        val variants = listOf(
            "stop instagram", "disable instagram",
            "don't let me use instagram", "lock instagram"
        )
        variants.forEach { input ->
            val cmd = CommandParser.parse(input)
            assertEquals("Failed for: $input", CommandAction.BLOCK_APP, cmd.action)
        }
    }

    @Test fun `mute notifications`() {
        val cmd = CommandParser.parse("Mute all social apps for 2 hours")
        assertEquals(CommandAction.MUTE_NOTIFICATIONS, cmd.action)
        assertEquals(7200L, cmd.durationSeconds)
    }

    @Test fun `study mode`() {
        val cmd = CommandParser.parse("Study mode for 3 hours")
        assertEquals(CommandAction.ACTIVATE_MODE, cmd.action)
        assertEquals(FocusMode.STUDY, cmd.mode)
        assertEquals(10800L, cmd.durationSeconds)
    }

    @Test fun `when open trigger`() {
        val cmd = CommandParser.parse("When I open Facebook, block it after 10 minutes for 2 hours")
        assertEquals(CommandTrigger.ON_APP_OPEN, cmd.trigger)
        assertEquals("com.facebook.katana", cmd.triggerApp)
    }

    @Test fun `for a while = 1 hour`() {
        val duration = CommandParser.parseDuration("block instagram for a while")
        assertEquals(3600L, duration)
    }

    @Test fun `duration parsing - minutes`() {
        val duration = CommandParser.parseDuration("for 30 minutes")
        assertEquals(1800L, duration)
    }

    @Test fun `duration parsing - hours and minutes`() {
        val duration = CommandParser.parseDuration("for 1 hour 30 minutes")
        assertEquals(5400L, duration)
    }

    @Test fun `empty command is invalid`() {
        val cmd = CommandParser.parse("")
        assertFalse(cmd.isValid)
    }
}
