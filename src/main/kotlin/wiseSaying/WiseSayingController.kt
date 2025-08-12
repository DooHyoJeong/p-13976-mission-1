package com.mysite.wiseSaying

class WiseSayingController(
) {
    private val wiseSayingService: WiseSayingService = WiseSayingService()

    fun register(saying: String, author: String): Int {
        if (wiseSayingService.create(saying, author) != null) {
            return wiseSayingService.getLastId()
        } else {
            throw IllegalArgumentException("명언 등록에 실패했습니다.")
        }
    }

    fun getWiseSayings(page: Int, keywordType: String?, keyword: String?)
            : Page<WiseSaying> = wiseSayingService.list(page, keywordType, keyword)

    fun getLastId(): Int {
        return wiseSayingService.getLastId()
    }

    fun findById(id: Int): WiseSaying? {
        return wiseSayingService.findById(id)
    }

    fun delete(id: Int): Boolean {
        return wiseSayingService.delete(id)
    }

    fun update(id: Int, newSaying: String, newAuthor: String): WiseSaying? {
        return wiseSayingService.update(id, newSaying, newAuthor)
    }
    fun build() : Boolean {

        return wiseSayingService.build()

    }
}