package com.mysite

import com.mysite.app.RealExiter
import com.mysite.app.WiseSayingApp
import com.mysite.wiseSaying.WiseSayingController


//TIP 코드를 <b>실행</b>하려면 <shortcut actionId="Run"/>을(를) 누르거나
// 에디터 여백에 있는 <icon src="AllIcons.Actions.Execute"/> 아이콘을 클릭하세요.
fun main() {
    val controller = WiseSayingController()

    val app = WiseSayingApp(
        exiter = RealExiter,
        input = System.`in`,
        out   = System.out,
        wiseSayingController = controller
    )
    app.run()
}