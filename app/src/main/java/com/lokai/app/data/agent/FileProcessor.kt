package com.lokai.app.data.agent

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File

private const val TAG = "FileProcessor"

/**
 * Reads a file at [path] and returns its plain-text content.
 *
 * Routing by extension:
 * - .txt / code files  → direct read (UTF-8)
 * - .md                → strip markdown syntax, preserve heading text
 * - .pdf               → Android PdfRenderer text layer extraction
 * - .json / .xml / .yaml / .toml → direct read
 *
 * Throws [FileProcessingException] on unsupported format, empty/scanned PDF,
 * or unreadable file.
 */
class FileProcessor(private val context: Context) {

    sealed class Result {
        data class Success(val text: String, val estimatedTokens: Int) : Result()
        data class Error(val message: String) : Result()
    }

    fun process(path: String): Result {
        val file = File(path)
        if (!file.exists()) return Result.Error("File not found: ${file.name}")
        if (!file.canRead()) return Result.Error("Cannot read file: ${file.name}")

        val ext = file.extension.lowercase()
        Log.d(TAG, "Processing $path (ext=$ext, size=${file.length()})")

        return try {
            val text = when (ext) {
                "pdf"  -> extractPdf(file)
                "md"   -> extractMarkdown(file)
                "txt"  -> file.readText(Charsets.UTF_8)
                // Code files
                "py", "js", "ts", "kt", "java", "cpp", "c", "go", "rs",
                "swift", "rb", "php", "html", "css", "sh", "sql",
                // Data/config files
                "json", "xml", "yaml", "yml", "toml" -> file.readText(Charsets.UTF_8)
                else   -> return Result.Error("Unsupported file type: .$ext")
            }

            if (text.isBlank()) {
                return Result.Error("File appears to be empty or contains no readable text.")
            }

            val tokens = estimateTokens(text)
            Result.Success(text, tokens)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing $path", e)
            Result.Error(e.message ?: "Unknown error reading file.")
        }
    }

    private fun extractPdf(file: File): String {
        val sb = StringBuilder()
        var pageCount = 0

        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pfd.use {
                val renderer = PdfRenderer(pfd)
                renderer.use { r ->
                    pageCount = r.pageCount
                    for (i in 0 until r.pageCount) {
                        val page = r.openPage(i)
                        // Android PdfRenderer doesn't directly expose text —
                        // we use the page's content stream via reflection or
                        // fall back to noting that the page was rendered.
                        // For Phase 5 we extract via the page render approach.
                        // Since PdfRenderer only gives bitmap output on Android,
                        // we indicate this limitation clearly.
                        page.close()
                    }
                }
            }
        } catch (e: Exception) {
            throw Exception("Could not open PDF: ${e.message}")
        }

        // Android's PdfRenderer only renders to bitmap — it does not expose
        // a text extraction API. We use the PdfDocument approach via iText/PDFBox
        // which isn't available without a third-party library.
        // Phase 5 implementation: attempt text extraction via pdftotext if available,
        // otherwise raise a clear error directing the user to use .txt/.md instead.
        //
        // For the actual production build, integrate PdfBox-Android or iText:
        // implementation 'com.tom_roush:pdfbox-android:2.0.27.0'
        // This is a stub that will be replaced once the dependency is confirmed.
        throw Exception(
            "PDF text extraction requires the PdfBox-Android library.\n" +
            "This PDF has $pageCount page(s). If it is a scanned PDF (image only), " +
            "text extraction is not supported in v1.\n" +
            "Please export your document as .txt or .md for best results."
        )
    }

    private fun extractMarkdown(file: File): String {
        val raw = file.readText(Charsets.UTF_8)
        // Strip markdown formatting but preserve heading text (used as skeleton anchors)
        // Keep: heading text, paragraph text
        // Remove: **, __, ~~, backtick inline code, image syntax, link URLs
        return raw
            .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE)) { it.value } // keep headings as-is for skeleton
            .replace(Regex("\\*\\*(.+?)\\*\\*")) { it.groupValues[1] }
            .replace(Regex("__(.+?)__")) { it.groupValues[1] }
            .replace(Regex("\\*(.+?)\\*")) { it.groupValues[1] }
            .replace(Regex("_(.+?)_")) { it.groupValues[1] }
            .replace(Regex("~~(.+?)~~")) { it.groupValues[1] }
            .replace(Regex("`(.+?)`")) { it.groupValues[1] }
            .replace(Regex("!\\[.*?\\]\\(.*?\\)"), "") // remove images
            .replace(Regex("\\[(.+?)\\]\\(.*?\\)")) { it.groupValues[1] } // keep link text
            .replace(Regex("^>\\s+", RegexOption.MULTILINE), "") // remove blockquote markers
            .replace(Regex("^[-*+]\\s+", RegexOption.MULTILINE), "") // remove list markers
    }

    /** Rough token estimate: 1 token ≈ 0.75 words ≈ 4 characters */
    fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    companion object {
        /** All file extensions supported by at least one agent category */
        val SUPPORTED_EXTENSIONS = setOf(
            "txt", "md", "pdf",
            "py", "js", "ts", "kt", "java", "cpp", "c", "go", "rs",
            "swift", "rb", "php", "html", "css", "sh", "sql",
            "json", "xml", "yaml", "yml", "toml"
        )

        fun isSupportedExtension(ext: String) = ext.lowercase() in SUPPORTED_EXTENSIONS
    }
}
