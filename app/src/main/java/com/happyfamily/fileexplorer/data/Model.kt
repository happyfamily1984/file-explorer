package com.happyfamily.fileexplorer.data

import java.io.File

/** 파일 종류 — 형식별 정렬 시 이 순서대로 묶어서 보여준다. */
enum class FileKind(val label: String) {
    FOLDER("폴더"),
    IMAGE("이미지"),
    VIDEO("동영상"),
    AUDIO("오디오"),
    DOCUMENT("문서"),
    ARCHIVE("압축 파일"),
    APK("설치 파일"),
    OTHER("기타");

    companion object {
        private val image = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "dng")
        private val video = setOf("mp4", "mkv", "avi", "mov", "wmv", "webm", "3gp", "m4v", "ts", "flv")
        private val audio = setOf("mp3", "wav", "m4a", "aac", "flac", "ogg", "wma", "amr", "opus", "mid")
        private val doc = setOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "hwp", "hwpx", "csv",
            "rtf", "odt", "ods", "odp", "md", "html", "htm", "json", "xml", "epub"
        )
        private val archive = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "alz", "egg")

        fun of(name: String, isDir: Boolean): FileKind {
            if (isDir) return FOLDER
            val ext = name.substringAfterLast('.', "").lowercase()
            return when (ext) {
                in image -> IMAGE
                in video -> VIDEO
                in audio -> AUDIO
                in doc -> DOCUMENT
                in archive -> ARCHIVE
                "apk", "apks", "xapk" -> APK
                else -> OTHER
            }
        }
    }
}

data class FileItem(
    val path: String,
    val name: String,
    val isDir: Boolean,
    val size: Long,
    val modified: Long,
    /** 폴더일 때 하위 항목 수 */
    val childCount: Int = 0,
) {
    val kind: FileKind get() = FileKind.of(name, isDir)
    val extension: String get() = if (isDir) "" else name.substringAfterLast('.', "").lowercase()
    val file: File get() = File(path)

    companion object {
        fun from(f: File, showHidden: Boolean = false): FileItem {
            val dir = f.isDirectory
            return FileItem(
                path = f.absolutePath,
                name = f.name,
                isDir = dir,
                size = if (dir) 0L else f.length(),
                modified = f.lastModified(),
                childCount = if (dir) (f.list()?.count { showHidden || !it.startsWith(".") } ?: 0) else 0,
            )
        }
    }
}

/** 카테고리(홈 화면의 6개 타일) */
enum class Category(val label: String) {
    IMAGE("이미지"),
    VIDEO("동영상"),
    AUDIO("오디오 파일"),
    DOCUMENT("문서"),
    DOWNLOAD("다운로드"),
    APK("설치 파일"),
}

data class StorageInfo(val used: Long, val total: Long)
