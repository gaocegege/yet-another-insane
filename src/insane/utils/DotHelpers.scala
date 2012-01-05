package insane
package utils

object DotHelpers {
  private var _nextName   = 0
  private var _nextColor  = 0

  private def nextName = {
    _nextName += 1
    _nextName.toString
  }

  private var names = Map[AnyRef, String]()

  def uniqueName(obj: AnyRef) = {
    if (!names.contains(obj)) {
      names = names + (obj -> nextName)
    }

    names(obj)
  }

  val bgColors = List("bisque", "khaki", "mistyrose", "lightcyan", "mediumorchid", "aquamarine", "antiquewhite")

  def nextColor = {
    _nextColor += 1
    val colornumber: String = if((_nextColor/bgColors.size)%3 == 0) "" else ((_nextColor/bgColors.size)%3)+"";
    bgColors(_nextColor%bgColors.size)+colornumber
  }

  def escape(s: String) =
    s.replaceAll("\\\\n", "__NEWLINE__").replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"").replaceAll("\\\n", "\\\\n").replaceAll("[^<>@a-zA-Z0-9;$.,!# \t=^:_\\\\\"'*+/&()\\[\\]{}-]", "?").replaceAll("__NEWLINE__", "\\\\n")

  def labeledArrow(x: String, label: String, y: String, options: List[String] = Nil) =
    arrow(x, y, "label=\""+escape(label)+"\"" :: options)

  def labeledDashedArrow(x: String, label: String, y: String, options: List[String] = Nil) =
    arrow(x, y, "label=\""+escape(label)+"\"" :: "style=dashed" :: options)

  def arrow(x: String, y: String, options: List[String] = Nil) = {
    "  "+x+" -> "+y+options.mkString(" [", " ", "]")+";\n"
  }

  def box(id : String, name : String, options: List[String] = Nil) = {
    node(id, name, "shape=box" :: "color=lightblue" :: "style=filled" :: options)
  }

  def invisNode(id : String, name : String, options: List[String] = Nil) = {
    node(id, name, "shape=none" :: options)
  }

  def dashedNode(id : String, name : String, options: List[String] = Nil) = {
    node(id, name, "style=dashed" :: options)
  }

  def node(id: String, name: String, options: List[String] = Nil) = {
    id +("label=\""+escape(name)+"\"" :: options).mkString(" [", ", ", "]")+";\n"
  }
}
