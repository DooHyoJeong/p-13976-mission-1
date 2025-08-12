import com.mysite.app.Exiter
import com.mysite.app.WiseSayingApp
import com.mysite.wiseSaying.WiseSayingController
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class TestExit(val code: Int) : RuntimeException()

class FakeExiter : Exiter {
    override fun exit(code: Int): Nothing {
        throw TestExit(code)
    }
}

class WiseSayingAppTest {

    private fun run(script: String): String {

        val inStream = ByteArrayInputStream(script.toByteArray(StandardCharsets.UTF_8))
        val outBuf = ByteArrayOutputStream()
        val controller = WiseSayingController()
        val app  = WiseSayingApp(FakeExiter(), inStream, PrintStream(outBuf, true),controller)

        assertFailsWith<TestExit> { app.run() }
        return outBuf.toString(StandardCharsets.UTF_8)
    }
    private fun deleteRecursively(pathString: String) {
        val path = Paths.get(pathString)
//        println("DELETE DIR = ${path.toAbsolutePath()}")

        if (Files.notExists(path)) return
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .use { stream ->                       // ← 중요!
                stream.forEach { Files.deleteIfExists(it) }
            }
    }
    @Test
    fun step1_exit() {
        val out = run("종료\n")           // ← 기존 run(script) 헬퍼 사용
        assertTrue(out.contains("앱을 종료합니다."))
    }

    // 3단계: 등록시 생성된 명언번호 노출
    @Test
    fun step2and3_register_shows_created_id() {
        deleteRecursively(Paths.get("db").resolve("wiseSaying").toString())

        val out = run(
            """
            등록
            현재를 사랑하라.
            작자미상
            종료
            """.trimIndent()
        )
        assertTrue(out.contains("1번 명언이 등록되었습니다."))

    }
    // 4단계: 등록마다 증가
    @Test
    fun step4_register_increments_id() {
        deleteRecursively(Paths.get("db").resolve("wiseSaying").toString())

        val out = run(
            """
            등록
            현재를 사랑하라.
            작자미상
            등록
            현재를 사랑하라.
            작자미상
            종료
            """.trimIndent()
        )
        assertTrue(out.contains("1번 명언이 등록되었습니다."))
        assertTrue(out.contains("2번 명언이 등록되었습니다."))
        assertTrue(out.indexOf("1번 명언이 등록되었습니다.") < out.indexOf("2번 명언이 등록되었습니다."))
    }

    // 5단계: 목록(역순 출력)
    @Test
    fun step5_list_shows_desc_order() {
        deleteRecursively(Paths.get("db").resolve("wiseSaying").toString())

        val out = run(
            """
            등록
            현재를 사랑하라.
            작자미상
            등록
            과거에 집착하지 마라.
            작자미상
            목록
            종료
            """.trimIndent()
        )
        assertTrue(out.contains("번호 / 작가 / 명언"))
        val i2 = out.indexOf("2 / 작자미상 / 과거에 집착하지 마라.")
        val i1 = out.indexOf("1 / 작자미상 / 현재를 사랑하라.")
        assertTrue(i2 >= 0 && i1 >= 0 && i2 < i1, "역순(2가 1보다 위)으로 나오지 않음:\n$out")
    }

    // 6단계: 삭제
    @Test
    fun step6_delete() {
        deleteRecursively(Paths.get("db").resolve("wiseSaying").toString())

        val out = run(
            """
            등록
            현재를 사랑하라.
            작자미상
            등록
            과거에 집착하지 마라.
            작자미상
            목록
            삭제?id=1
            종료
            """.trimIndent()
        )
        assertTrue(out.contains("1번 명언이 삭제되었습니다."))
    }

    // 7단계: 없는 것 삭제 예외, ID 재사용 금지
    @Test
    fun step7_delete_not_exists_and_no_id_reuse() {
        deleteRecursively(Paths.get("db").resolve("wiseSaying").toString())

        val out = run(
            """
            등록
            현재를 사랑하라.
            작자미상
            등록
            과거에 집착하지 마라.
            작자미상
            목록
            삭제?id=1
            삭제?id=1
            등록
            세 번째도 소중하다.
            작자미상
            종료
            """.trimIndent()
        )
        assertTrue(out.contains("1번 명언이 삭제되었습니다."))
        assertTrue(out.contains("1번 명언은 존재하지 않습니다."))
        // 재등록 시 3번이 되어야 함(재사용 금지)
        assertTrue(out.contains("3번 명언이 등록되었습니다."))
    }

    // 8단계: 수정
    @Test
    fun step8_modify_flow() {
        deleteRecursively(Paths.get("db").resolve("wiseSaying").toString())

        val out = run(
            """
            등록
            현재를 사랑하라.
            작자미상
            등록
            과거에 집착하지 마라.
            작자미상
            목록
            삭제?id=1
            삭제?id=1
            수정?id=3
            수정?id=2
            현재와 자신을 사랑하라.
            홍길동
            목록
            종료
            """.trimIndent()
        )
        assertTrue(out.contains("3번 명언은 존재하지 않습니다."))
        assertTrue(out.contains("명언(기존) : 과거에 집착하지 마라."))
        assertTrue(out.contains("작가(기존) : 작자미상"))
        assertTrue(out.contains("2 / 홍길동 / 현재와 자신을 사랑하라."))
    }

    // 9단계: 파일 영속성 (개별 json, lastId.txt)
    @Test
    fun step9_file_persistence() {
        deleteRecursively(Paths.get("db").resolve("wiseSaying").toString())

        val base = Paths.get("db")

        // 1차 실행: 등록 2개 후 종료
        run(
            """
            등록
            현재를 사랑하라.
            작자미상
            등록
            과거에 집착하지 마라.
            작자미상
            종료
            """.trimIndent()
        )

        // 파일 존재/내용 확인
        val dir = Paths.get("db").resolve("wiseSaying")
        val f1 = dir.resolve("1.json")
        val f2 = dir.resolve("2.json")
        val last = dir.resolve("lastId.txt")
        assertTrue(Files.exists(f1) && Files.exists(f2) && Files.exists(last), "파일 구조가 없음")

        val j1 = Files.readString(f1)
        val j2 = Files.readString(f2)
        val lastId = Files.readString(last).trim()
        assertTrue(j1.contains("\"id\": 1") && j1.contains("\"content\":") && j1.contains("\"author\":"), j1)
        assertTrue(j2.contains("\"id\": 2"))
        assertEquals("2", lastId)

        // 2차 실행: 재시작 후 목록 확인(영속성 검증)
        val out2 = run(
            """
            목록
            종료
            """.trimIndent()
        )
        val i2 = out2.indexOf("2 /")
        val i1 = out2.indexOf("1 /")
        assertTrue(i2 >= 0 && i1 >= 0 && i2 < i1, "재시작 후 목록 역순 미유지:\n$out2")
    }

    // 10단계: 빌드 -> data.json 생성/갱신
    @Test
    fun step10_build_data_json() {
        deleteRecursively(Paths.get("db").resolve("wiseSaying").toString())

        val out = run(
            """
            등록
            현재를 사랑하라.
            작자미상
            등록
            과거에 집착하지 마라.
            작자미상
            목록
            삭제?id=1
            삭제?id=1
            수정?id=2
            현재와 자신을 사랑하라.
            홍길동
            목록
            빌드
            종료
            """.trimIndent()
        )
        assertTrue(out.contains("data.json 파일의 내용이 갱신되었습니다."))
        val dataJson = Paths.get("db").resolve("wiseSaying").resolve("data.json")

        assertTrue(Files.exists(dataJson), "data.json이 생성되지 않음")
        val json = Files.readString(dataJson)
        // 살아있는 2번의 수정된 내용이 반영되어야 함
        assertTrue(json.contains("\"id\": 2"))
        assertTrue(json.contains("현재와 자신을 사랑하라."))
        assertTrue(json.contains("홍길동"))
        // 삭제된 1번은 없어야 함(간단 검증)
        assertTrue(!json.contains("\"id\": 1") || !json.contains("명언 1"))
    }

    // 13단계: 검색 (content/author)
    @Test
    fun step13_search() {
        deleteRecursively(Paths.get("db").resolve("wiseSaying").toString())

        val out = run(
            """
            등록
            현재를 사랑하라.
            작자미상
            등록
            과거에 집착하지 마라.
            작자미상
            목록?keywordType=content&keyword=과거
            목록?keywordType=author&keyword=작자
            종료
            """.trimIndent()
        )
        // content 검색
        val sect1 = out.substring(out.indexOf("검색타입 : content"))
        assertTrue(sect1.contains("번호 / 작가 / 명언"))
        assertTrue(sect1.contains("2 / 작자미상 / 과거에 집착하지 마라."))
        // author 검색
        val sect2 = out.substring(out.indexOf("검색타입 : author"))
        assertTrue(sect2.contains("2 / 작자미상 / 과거에 집착하지 마라."))
        assertTrue(sect2.contains("1 / 작자미상 / 현재를 사랑하라."))
    }

    // 14단계: 페이징 (10개 등록 후 5개씩, 최신 우선)
    @Test
    fun step14_paging() {
        deleteRecursively(Paths.get("db").resolve("wiseSaying").toString())

        val sb = StringBuilder()
        for (i in 1..10) {
            sb.appendLine("등록")
            sb.appendLine("명언 $i")
            sb.appendLine("작자미상 $i")
        }
        sb.appendLine("목록")
        sb.appendLine("목록?page=2")
        sb.appendLine("종료")

        val out = run(sb.toString().trim()).replace("\r\n", "\n")

        // 목록 블록 두 개를 머리글로 분할
        val anchors = Regex("""(?m)^번호 / 작가 / 명언$""").findAll(out).map { it.range.first }.toList()
        assertTrue(anchors.size >= 2, "목록 출력이 2번 나오지 않았습니다:\n$out")

        val firstBlock  = out.substring(anchors[0], anchors[1]) // 1페이지(10~6)
        val secondBlock = out.substring(anchors[1])             // 2페이지(5~1)

        // 1페이지: 10~6
        assertTrue(firstBlock.contains("10 / 작자미상 10 / 명언 10"), firstBlock)
        assertTrue(firstBlock.contains("6 / 작자미상 6 / 명언 6"), firstBlock)

        // 2페이지: 5~1
        assertTrue(secondBlock.contains("5 / 작자미상 5 / 명언 5"), secondBlock)
        assertTrue(secondBlock.contains("1 / 작자미상 1 / 명언 1"), secondBlock)

        // 페이지 라인 검증 (라인 전체를 정규식으로 추출)
        val pageLines = Regex("""(?m)^페이지\s*:\s*(.+)$""").findAll(out).map { it.groupValues[1] }.toList()
        assertTrue(pageLines.size >= 2, "페이지 표기가 2번 나오지 않음:\n$out")
        assertTrue(pageLines[0].contains("[1]"), pageLines[0]) // 1페이지 표시
        assertTrue(pageLines[1].contains("[2]"), pageLines[1]) // 2페이지 표시
    }
}