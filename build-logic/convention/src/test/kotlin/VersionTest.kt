import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VersionTest {
    @Test fun encodesVersion() {
        assertEquals(10001, DeftVersion.parse("0.1.0-1").code)
        assertEquals(1020304, DeftVersion.parse("1.2.3-4").code)
        assertEquals("1.2.3", DeftVersion.parse("1.2.3-4").name)
    }
    @Test fun rejectsInvalidAndCollidingComponents() {
        listOf("0.1.0-0", "0.1.0-100", "0.100.0-1", "0.1.100-1", "1.2.3", "x").forEach {
            assertThrows(IllegalArgumentException::class.java) { DeftVersion.parse(it) }
        }
    }
}
