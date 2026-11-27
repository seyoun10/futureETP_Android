package io.github.seyoon.futureetp

import com.lemonappdev.konsist.api.Konsist
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * import 방향(D-041 대응, AOS-010)
 * - feature끼리 import하지 않는다
 * - core는 feature·navigation을 import하지 않는다
 * - feature는 navigation을 import하지 않는다
 */
class ArchitectureTest {
    private val root = "io.github.seyoon.futureetp"
    private val files = Konsist.scopeFromProduction().files

    /** `...feature.<이름>.…` 패키지면 feature 이름, 아니면 null */
    private fun featureOf(pkg: String): String? {
        val prefix = "$root.feature."
        return if (pkg.startsWith(prefix)) pkg.removePrefix(prefix).substringBefore('.') else null
    }

    @Test
    fun featuresDoNotImportOtherFeatures() {
        val violations =
            files.flatMap { file ->
                val own = featureOf(file.packagee?.name.orEmpty()) ?: return@flatMap emptyList()
                file.imports
                    .filter { featureOf(it.name)?.let { other -> other != own } == true }
                    .map { "${file.path}: ${it.name}" }
            }
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun coreDoesNotImportFeaturesOrNavigation() {
        val violations =
            files
                .filter {
                    it.packagee
                        ?.name
                        .orEmpty()
                        .startsWith("$root.core")
                }.flatMap { file ->
                    file.imports
                        .filter { it.name.startsWith("$root.feature.") || it.name.startsWith("$root.navigation") }
                        .map { "${file.path}: ${it.name}" }
                }
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun featuresDoNotImportNavigation() {
        val violations =
            files
                .filter { featureOf(it.packagee?.name.orEmpty()) != null }
                .flatMap { file ->
                    file.imports
                        .filter { it.name.startsWith("$root.navigation") }
                        .map { "${file.path}: ${it.name}" }
                }
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun scopeFindsSources() {
        assertTrue(files.any { it.packagee?.name == "$root.feature.settings" })
    }
}
