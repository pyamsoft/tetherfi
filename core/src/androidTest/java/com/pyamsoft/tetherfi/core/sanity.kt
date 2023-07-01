import androidx.test.filters.SmallTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

@SmallTest
class AndroidSanity {

  @Test fun sanity() = runTest { assertEquals(3 + 3, 6) }
}
