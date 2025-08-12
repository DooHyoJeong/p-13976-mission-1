package com.mysite.app

import com.mysite.command.Command
import com.mysite.wiseSaying.WiseSayingController
import java.io.InputStream
import java.io.PrintStream
import java.nio.file.Path
import kotlin.system.exitProcess

interface Exiter { fun exit(code: Int): Nothing }
object RealExiter : Exiter {
    override fun exit(code: Int): Nothing = exitProcess(code)
}
class WiseSayingApp(
    private val exiter: Exiter = RealExiter,
    private val input: InputStream = System.`in`,
    private val out: PrintStream = System.out

) {
    private val wiseSayingController = WiseSayingController()
    fun run() {
        val br = input.bufferedReader()

        while (true) {
            out.println("== 명언 앱 ==")
            out.print("명령) ")
            val line = br.readLine() ?: continue
            val cmd = Command.parse(line)
            when(cmd.input)  {
                "종료" -> {
                    out.println("앱을 종료합니다.")
                    exiter.exit(0)              // 테스트에선 FakeExiter가 예외 던짐
                }
                "등록" -> {
                    out.print("명언 : ")
                    val saying = br.readLine() ?: return
                    out.print("작가 : ")
                    val author = br.readLine() ?: return
                    wiseSayingController.register(saying, author)
                    val id = wiseSayingController.getLastId();
                    out.println("${id}번 명언이 등록되었습니다. \"$saying\" - $author")
                }
                "목록" -> {
                    out.println()

                    val page = cmd.getInt("page") ?: 1
                    val keywordType = cmd.get("keywordType")
                    val keyword = cmd.get("keyword")
                    // 검색 옵션이 있으면 검색 섹션 먼저
                    if (!keyword.isNullOrBlank() && !keywordType.isNullOrBlank()) {
                        out.println("----------------------")
                        out.println("검색타입 : $keywordType")
                        out.println("검색어 : $keyword")
                        out.println("----------------------")
                    }

                    val res = wiseSayingController.getWiseSayings(page, keywordType, keyword)

                    out.println("번호 / 작가 / 명언")
                    out.println("----------------------")
                    res.items.forEach { v ->
                        out.println("${v.id} / ${v.author} / ${v.saying}")
                    }
                    out.println("----------------------")
                    val pageLine = (1..res.totalPages).joinToString(" / ") { p ->
                        if (p == res.page) "[$p]" else "$p"
                    }
                    out.println("페이지 : $pageLine")
                }
                "삭제" -> {
                    val id = cmd.getInt("id")
                    if (id == null) {
                        out.println("삭제할 명언의 ID를 입력해주세요.")
                    } else {
                        if (!wiseSayingController.delete(id)) {
                            out.println("${id}번 명언은 존재하지 않습니다.")
                            continue
                        }
                        out.println("${id}번 명언이 삭제되었습니다.")
                    }
                }
                "수정" -> {
                    val id = cmd.getInt("id")
                    if (id == null) {
                        out.println("수정할 명언의 ID를 입력해주세요.")
                    } else {
                        val cur = wiseSayingController.findById(id)
                        if (cur == null) {
                            out.println("${id}번 명언은 존재하지 않습니다.")
                            continue
                        }

                        cur.let { out.println("명언(기존) : ${it.saying}") }
                        out.print("명언 : ")
                        val newSaying = br.readLine() ?: continue
                        cur.let { out.println("작가(기존) : ${it.author}") }
                        out.print("작가 : ")
                        val newAuthor = br.readLine() ?: continue
                        val updatedSaying = wiseSayingController.update(id, newSaying, newAuthor)

                        updatedSaying?.let { out.println("명언 ID $id 가 수정되었습니다: ${it.id} / \"${updatedSaying.author}\" / ${updatedSaying.saying}") }
                    }
                }
                "빌드" -> {
                    if(wiseSayingController.build()){
                        out.println("data.json 파일의 내용이 갱신되었습니다.")
                    } else {
                        out.println("빌드에 실패했습니다.")
                    }
                }
                else -> {
                    out.println("알 수 없는 명령입니다: ${cmd.input}")
                }
            }
        }
    }
}