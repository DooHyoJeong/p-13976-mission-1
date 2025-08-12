package com.mysite.wiseSaying

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.text.Charsets.UTF_8

interface IdSequence { fun nextId(): Int }

class FileIdSequence(private val lastIdFile: Path) : IdSequence {
    private var lastId: Int = run {
        if (Files.exists(lastIdFile)) Files.readString(lastIdFile, UTF_8).trim().toIntOrNull() ?: 0
        else 0
    }

    override fun nextId(): Int {
        lastId += 1
        Files.createDirectories(lastIdFile.parent)
        Files.writeString(lastIdFile, lastId.toString(), UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        return lastId
    }

    fun setIfHigher(v: Int) {
        if (v > lastId) {
            lastId = v
            Files.createDirectories(lastIdFile.parent)
            Files.writeString(lastIdFile, lastId.toString(), UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        }
    }

    fun current(): Int = lastId
}
class WiseSayingRepository(
) {
    private val baseDir: Path = Paths.get(".")
    private val dir: Path = baseDir.resolve("db").resolve("wiseSaying")
    private val lastIdFile: Path = dir.resolve("lastId.txt")
    private val idSeq = FileIdSequence(lastIdFile)

    private val store = linkedMapOf<Int, WiseSaying>()
    init {
        Files.createDirectories(dir)
        Files.list(dir).use { stream ->
            stream.filter { it.fileName.toString().endsWith(".json") }
                .forEach { path ->
                    parseJson(Files.readString(path, UTF_8))?.let { ws ->
                        store[ws.id!!] = ws
                    }
                }
        }
        val maxId = store.keys.maxOrNull() ?: 0
        idSeq.setIfHigher(maxId)
    }
    fun create(content: String, author: String): WiseSaying {
        val id = idSeq.nextId()
        val ws = WiseSaying(id = id, saying = content.trim(), author = author.trim())
        store[id] = ws
        writeJson(ws)
        return ws
    }

    fun findAllDesc(): List<WiseSaying> = store.values.sortedByDescending { it.id!! }

    fun getLastId(): Int = idSeq.current()

    fun findById(id: Int): WiseSaying? = store[id]

    fun update(id: Int, newSaying: String, newAuthor: String): WiseSaying? {
        val ws = store[id] ?: return null
        ws.saying = newSaying.trim()
        ws.author = newAuthor.trim()
        writeJson(ws)             // 수정 시 파일 갱신
        return ws
    }

    fun delete(id: Int): Boolean {
        val removed = store.remove(id) != null
        if (removed) Files.deleteIfExists(dir.resolve("$id.json"))
        return removed
    }


    private fun writeJson(ws: WiseSaying) {
        val json = """
            {
              "id": ${ws.id},
              "content": ${escape(ws.saying)},
              "author": ${escape(ws.author)}
            }
        """.trimIndent() + "\n"
        Files.writeString(dir.resolve("${ws.id}.json"), json, UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    private fun parseJson(raw: String): WiseSaying? {
        val id = Regex("\"id\"\\s*:\\s*(\\d+)").find(raw)?.groupValues?.get(1)?.toIntOrNull()
        val content = Regex("\"content\"\\s*:\\s*\"(.*)\"").find(raw)?.groupValues?.get(1)
            ?.replace("\\n", "\n")?.replace("\\r", "\r")?.replace("\\\"", "\"")?.replace("\\\\", "\\")
        val author = Regex("\"author\"\\s*:\\s*\"(.*)\"").find(raw)?.groupValues?.get(1)
            ?.replace("\\n", "\n")?.replace("\\r", "\r")?.replace("\\\"", "\"")?.replace("\\\\", "\\")
        return if (id != null && content != null && author != null) WiseSaying(id, content, author) else null
    }

    private fun escape(s: String): String {
        val e = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n")
        return "\"$e\""
    }

    fun build(): Boolean {
        val list = store.values.sortedBy { it.id ?: Int.MAX_VALUE }

        val sb = StringBuilder()
        sb.append("[\n")
        list.forEachIndexed { i, ws ->
            sb.append("  {\n")
            sb.append("    \"id\": ").append(ws.id).append(",\n")
            sb.append("    \"content\": ").append(escape(ws.saying)).append(",\n")
            sb.append("    \"author\": ").append(escape(ws.author)).append("\n")
            sb.append("  }")
            if (i != list.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append("]\n")

        val dataJson = dir.resolve("data.json")
        Files.writeString(
            dataJson,
            sb.toString(),
            UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
        return true
    }
}