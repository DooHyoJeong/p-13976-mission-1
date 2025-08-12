package com.mysite.wiseSaying
data class Page<T>(val items: List<T>, val page: Int, val totalPages: Int)

class WiseSayingService(
) {
    private val wiseSayingRepository: WiseSayingRepository = WiseSayingRepository()

    fun create(saying: String, author: String): Int {
        return wiseSayingRepository.create(saying, author).id!!   // id만 반환
    }

    fun findAllDesc(): List<WiseSaying> {
        return wiseSayingRepository.findAllDesc()
    }

    fun getLastId(): Int {
        return wiseSayingRepository.getLastId()
    }

    fun findById(id: Int): WiseSaying? {
        return wiseSayingRepository.findById(id)
    }

    fun delete(id: Int): Boolean {
        return wiseSayingRepository.delete(id)
    }

    fun update(id: Int, newSaying: String, newAuthor: String): WiseSaying? {
        return wiseSayingRepository.update(id, newSaying, newAuthor)
    }

    fun build(): Boolean {
        return wiseSayingRepository.build()
    }

    fun list(page: Int, keywordType: String?, keyword: String?, pageSize: Int = 5): Page<WiseSaying> {
        // 전체(내림차순)
        var all = wiseSayingRepository.findAllDesc()

        // (선택) 검색
        if (!keyword.isNullOrBlank()) {
            all = when (keywordType) {
                "author"  -> all.filter { it.author.contains(keyword) }
                "content" -> all.filter { it.saying.contains(keyword) }
                else      -> all
            }
        }

        val totalPages = ((all.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        val safePage   = page.coerceIn(1, totalPages)
        val from       = (safePage - 1) * pageSize
        val to         = minOf(from + pageSize, all.size)
        val items      = if (from < to) all.subList(from, to) else emptyList()

        return Page(items, safePage, totalPages)
    }
}