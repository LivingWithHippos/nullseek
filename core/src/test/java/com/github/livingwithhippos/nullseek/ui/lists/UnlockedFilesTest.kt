package com.github.livingwithhippos.nullseek.ui.lists

import com.github.livingwithhippos.nullseek.core.domain.DebridError
import com.github.livingwithhippos.nullseek.core.domain.DebridFile
import com.github.livingwithhippos.nullseek.core.domain.DebridLink
import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockedFilesTest {
    private fun file(
        id: Int,
        name: String,
        selected: Boolean = true,
    ) = DebridFile(id = id, name = "Show/$name", shortName = name, size = 10, mimetype = null, selected = selected)

    private val files =
        listOf(
            file(1, "ep1.mkv"),
            file(2, "ep1.srt"),
            file(3, "theme.mp3"),
            file(4, "info.nfo"),
            file(5, "ep2.mkv", selected = false),
            file(6, "ep3.mp4"),
        )

    @Test
    fun mediaFilesAreVideoAndAudioOnly() {
        assertEquals(listOf(1, 3, 6), files.mediaFiles().map { it.id })
    }

    @Test
    fun filesAreUnlockedInOrderWithProgress() =
        runBlocking {
            val progress = mutableListOf<Pair<Int, Int>?>()
            var last: List<UnlockedFile> = emptyList()
            val outcome =
                unlockOneByOne(
                    targets = files.mediaFiles(),
                    existing = emptyList(),
                    fetch = { file ->
                        if (file.id == 3) {
                            DebridResult.Err(DebridError.Provider(35, "Infringing file"))
                        } else {
                            DebridResult.Ok(DebridLink("https://x/${file.id}"))
                        }
                    },
                    onUpdate = { list, p ->
                        last = list
                        progress.add(p)
                    },
                )
            assertEquals(2, outcome?.unlocked)
            assertEquals(listOf("Infringing file (error 35)"), outcome?.failures)
            assertEquals(listOf(1, 3, 6), last.map { it.fileId })
            assertEquals("Infringing file (error 35)", last[1].error)
            assertEquals(listOf(0 to 3, 1 to 3, 2 to 3, 3 to 3, null), progress)
        }

    @Test
    fun retriedFilesReplaceTheirFailure() =
        runBlocking {
            val existing =
                listOf(
                    UnlockedFile("ep1.mkv", 10, "https://x/1", null, null, fileId = 1),
                    UnlockedFile("theme.mp3", 10, "", null, null, fileId = 3, error = "Infringing file (error 35)"),
                )
            val targets = files.mediaFiles().notUnlockedIn(existing)
            assertEquals(listOf(3, 6), targets.map { it.id })
            var last: List<UnlockedFile> = emptyList()
            unlockOneByOne(
                targets = targets,
                existing = existing,
                fetch = { file -> DebridResult.Ok(DebridLink("https://x/${file.id}")) },
                onUpdate = { list, _ -> last = list },
            )
            assertEquals(listOf(1, 3, 6), last.map { it.fileId })
            assertTrue(last.none { it.failed })
        }

    @Test
    fun stopsWhenNoLongerCurrent() =
        runBlocking {
            var calls = 0
            val outcome =
                unlockOneByOne(
                    targets = files.mediaFiles(),
                    existing = emptyList(),
                    fetch = { file ->
                        calls++
                        DebridResult.Ok(DebridLink("https://x/${file.id}"))
                    },
                    onUpdate = { _, _ -> },
                    isStillCurrent = { calls < 2 },
                )
            assertNull(outcome)
            assertEquals(2, calls)
        }
}
