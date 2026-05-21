import com.github.noamm9.features.impl.general.storageoverlay.StoragePage
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

object StoragePageTest {
    @JvmStatic
    fun main(args: Array<String>) {
        assertEquals(StoragePage(4), StoragePage.enderchest(5))
        assertEquals(StoragePage(12), StoragePage.backpack(4))
        assertNotEquals(StoragePage(4), StoragePage(5))
    }
}