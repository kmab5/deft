import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NetworkIsolationCheckTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun identifiesEveryHttpClientFamilyWithoutRejectingLocalCoil() {
        listOf("com.squareup.okhttp3" to "okhttp", "com.squareup.retrofit2" to "retrofit", "com.android.volley" to "volley", "io.ktor" to "ktor-client-core-jvm", "io.coil-kt.coil3" to "coil-network-okhttp").forEach {
            assertTrue(forbiddenNetworkDependency(it.first, it.second))
        }
        assertFalse(forbiddenNetworkDependency("io.coil-kt.coil3", "coil-core"))
        assertFalse(forbiddenNetworkDependency("io.ktor", "ktor-io"))
    }

    @Test fun mergedManifestCannotEnableInternetWithoutMatching() {
        val manifest = temporary.newFile("AndroidManifest.xml")
        manifest.writeText("""<manifest xmlns:android="http://schemas.android.com/apk/res/android"><uses-permission android:name="android.permission.INTERNET"/></manifest>""")
        assertThrows(IllegalStateException::class.java) { validateReleaseNetworkManifest(manifest, false) }
        validateReleaseNetworkManifest(manifest, true)
        manifest.writeText("<manifest/>")
        validateReleaseNetworkManifest(manifest, false)
    }
}
