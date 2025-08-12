package com.mysite.command

import java.net.URLDecoder
import javax.management.Query.eq

class Command(
    val input : String,
    val params : Map<String, String> = emptyMap(),
) {
    companion object {
        fun parse(input: String): Command {
            val trimmed = input.trim()
            val parts = trimmed.split("?", limit = 2)
            val command = parts[0].trim()
            val params = if (parts.size > 1) {
                parts[1].split("&")
                    .filter { it.isNotBlank() }
                    .mapNotNull {
                        val eq = it.indexOf("=")
                        if(eq < 0) return@mapNotNull null
                        val k = it.substring(0 ,eq).trim()
                        val v = it.substring(eq + 1).trim()
                        k to URLDecoder.decode(v, "UTF-8")
                    }.toMap()
            }else {
                emptyMap()
            }
            return Command(command, params)
        }
    }
    fun get(input : String) : String? {
        return params[input]
    }
    fun getInt(input: String): Int? {
        return params[input]?.toIntOrNull()
    }
}