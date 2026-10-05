package com.gymora.data.transfer

/** Minimal RFC 4180 CSV reader/writer (quoted fields, escaped quotes, embedded newlines). */
object CsvCodec {

    fun formatRow(fields: List<String>): String = fields.joinToString(",") { escape(it) }

    private fun escape(field: String): String =
        if (field.any { it in SPECIAL }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }

    /** Parses [text] into rows of fields. Blank lines are skipped; a leading BOM is ignored. */
    fun parse(text: String): List<List<String>> = Parser(text.removePrefix(BOM)).run()

    private class Parser(private val text: String) {
        private val rows = mutableListOf<List<String>>()
        private var row = mutableListOf<String>()
        private val field = StringBuilder()
        private var inQuotes = false

        fun run(): List<List<String>> {
            var i = 0
            while (i < text.length) {
                if (inQuotes && text[i] == '"' && text.getOrNull(i + 1) == '"') {
                    field.append('"')
                    i++ // skip the second quote of the escaped pair
                } else {
                    consume(text[i])
                }
                i++
            }
            if (field.isNotEmpty() || row.isNotEmpty()) endRow()
            return rows
        }

        private fun consume(c: Char) {
            when {
                c == '"' -> inQuotes = !inQuotes
                inQuotes -> field.append(c)
                c == ',' -> endField()
                c == '\n' -> endRow()
                c != '\r' -> field.append(c)
            }
        }

        private fun endField() {
            row.add(field.toString())
            field.setLength(0)
        }

        private fun endRow() {
            endField()
            if (row.size > 1 || row[0].isNotEmpty()) rows.add(row)
            row = mutableListOf()
        }
    }

    private const val BOM = "﻿"
    private val SPECIAL = charArrayOf(',', '"', '\n', '\r')
}
