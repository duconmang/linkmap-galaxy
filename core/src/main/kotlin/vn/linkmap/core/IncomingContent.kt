package vn.linkmap.core

data class IncomingContent(val text:String,val urls:List<String>) {
    val kind:EntryKind get()=if(urls.isEmpty()) EntryKind.NOTE else EntryKind.LINK
    val suggestedTitle:String get()=text.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(120).orEmpty()
    companion object {
        const val MAX_LENGTH=64000
        fun detect(input:String):IncomingContent {
            val text=input.trim()
            require(text.isNotBlank()) { "Nhập hoặc dán nội dung cần lưu" }
            require(text.length<=MAX_LENGTH) { "Mỗi mục tối đa 64.000 ký tự; nội dung chưa được lưu" }
            val links=linkedSetOf<String>()
            Urls.fromInput(text)?.let { links.add(it) }
            links.addAll(Urls.extract(text))
            val explicitRanges=Regex("https?://[^\\s<>\"]+",RegexOption.IGNORE_CASE).findAll(text).map { it.range }.toList()
            // Scheme-less domains in prose; do not mistake email addresses or decimal numbers for URLs.
            Regex("(?<![\\w@/.-])(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+(?:com|org|net|io|dev|app|vn|edu|co|ai|me)(?::[0-9]{1,5})?(?:/[^\\s<>\"\\u0000-\\u001f]*)?(?![A-Za-z0-9-]|\\.[A-Za-z0-9])",RegexOption.IGNORE_CASE)
                .findAll(text).filter { match -> explicitRanges.none { match.range.first in it } }.forEach { match -> Urls.extract("https://${match.value}").singleOrNull()?.let { links.add(it) } }
            return IncomingContent(text,links.toList())
        }
    }
}
