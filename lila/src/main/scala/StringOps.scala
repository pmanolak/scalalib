package scalalib

import java.text.Normalizer

object StringOps:

  object slug:
    private val slugR              = """[^\w-]""".r
    private val slugMultiDashRegex = """-{2,}""".r

    def apply(input: String) =
      val nowhitespace = input.trim.replace(' ', '-')
      val singleDashes = slugMultiDashRegex.replaceAllIn(nowhitespace, "-")
      val normalized   = Normalizer.normalize(singleDashes, Normalizer.Form.NFD)
      val slug         = slugR.replaceAllIn(normalized, "")
      slug.toLowerCase

  private val onelineR                           = """\s+""".r
  def shorten(text: String, length: Int): String = shorten(text, length, "…")
  def shorten(text: String, length: Int, sep: String): String =
    val oneline = onelineR.replaceAllIn(text, " ")
    if oneline.lengthIs > length + sep.length then oneline.take(length) ++ sep
    else oneline

  def urlencode(str: String): String = java.net.URLEncoder.encode(str, "UTF-8")

  def addQueryParam(url: String, key: String, value: String): String = addQueryParams(url, Map(key -> value))
  def addQueryParams(url: String, params: Map[String, String]): String =
    if params.isEmpty then url
    else
      val queryString = params // we could encode the key, and we should, but is it really necessary?
        .map { (key, value) => s"$key=${urlencode(value)}" }
        .mkString("&")
      s"$url${if url.contains("?") then "&" else "?"}$queryString"

  def removeGarbageChars(str: String) = removeChars(str, isGarbageChar)

  private def removeChars(str: String, isRemoveable: Int => Boolean): String =
    if str.chars.anyMatch(isRemoveable(_)) then str.filterNot(isRemoveable(_)) else str

  private def isGarbageChar(c: Int) = c >= '\u0250' && (isInvisibleChar(c) ||
    // bunch of probably useless blocks https://www.compart.com/en/unicode/block/U+2100
    // but keep maths operators cause maths are cool https://www.compart.com/en/unicode/block/U+2200
    // and chess symbols https://www.compart.com/en/unicode/block/U+2600
    (c >= '\u2100' && c <= '\u21FF') ||
    (c >= '\u2300' && c <= '\u2653') ||
    (c >= '\u2660' && c <= '\u2C5F') ||
    // decorative chars ꧁ ꧂ and svastikas
    (c == '\ua9c1' || c == '\ua9c2' || c == '\u534d' || c == '\u5350') ||
    // pretty quranic chars ۩۞
    (c >= '\u06d6' && c <= '\u06ff') ||
    // phonetic extensions https://www.compart.com/en/unicode/block/U+1D00
    (c >= '\u1d00' && c <= '\u1d7f') ||
    // IPA extensions https://www.compart.com/en/unicode/block/U+0250
    // but allow https://www.compart.com/en/unicode/U+0259
    (c >= '\u0250' && c < '\u0259') || (c > '\u0259' && c <= '\u02af'))

  private inline def isInvisibleChar(c: Int) =
    // invisible chars https://www.compart.com/en/unicode/block/U+2000
    (c >= '\u2000' && c <= '\u200F') ||
      // weird stuff https://www.compart.com/en/unicode/block/U+2000
      (c >= '\u2028' && c <= '\u202F') ||
      // Hangul fillers
      (c == '\u115f' || c == '\u1160') ||
      // braille space https://unicode-explorer.com/c/2800
      (c == '\u2800')

  object normalize:

    private val ordinalRegex = "[º°ª½]".r

    // convert weird chars into letters when possible
    // but preserve ordinals
    def apply(str: String): String = Normalizer
      .normalize(
        ordinalRegex.replaceAllIn(
          str,
          _.group(0)(0) match
            case 'º' | '°' => "\u0001".toString
            case 'ª'       => '\u0002'.toString
            case '½'       => '\u0003'.toString
        ),
        Normalizer.Form.NFKC
      )
      .replace('\u0001', 'º')
      .replace('\u0002', 'ª')
      .replace('\u0003', '½')

  // https://www.compart.com/en/unicode/block/U+1F300
  // https://www.compart.com/en/unicode/block/U+1F600
  // https://www.compart.com/en/unicode/block/U+1F900
  private val multibyteSymbolsRegex =
    raw"[\p{So}\p{block=Emoticons}\p{block=Miscellaneous Symbols and Pictographs}\p{block=Supplemental Symbols and Pictographs}]".r
  def removeMultibyteSymbols(str: String): String = multibyteSymbolsRegex.replaceAllIn(str, "")

  // for publicly listed text like team names, study names, forum topics...
  def fullCleanUp(str: String) = removeMultibyteSymbols(removeChars(normalize(str), isGarbageChar)).trim

  // for inner text like study chapter names, possibly forum posts and team descriptions
  def softCleanUp(str: String) = removeChars(normalize(str), isInvisibleChar(_)).trim

  object base64:
    import java.util.Base64
    import java.nio.charset.StandardCharsets.UTF_8
    def encode(txt: String) =
      Base64.getEncoder.encodeToString(txt.getBytes(UTF_8))
    def decode(txt: String): Option[String] =
      try Some(new String(Base64.getDecoder.decode(txt), UTF_8))
      catch case _: java.lang.IllegalArgumentException => None
