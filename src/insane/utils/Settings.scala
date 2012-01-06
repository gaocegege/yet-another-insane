package insane
package utils

class Settings {
  var configPath                = "config.xml"

  var databaseType              = ""
  var databaseDSN               = ""
  var databaseUsername          = ""
  var databasePassword          = ""

  var verbosity                 = Verbosity.Normal

  def debugMode                 = verbosity == Verbosity.Debug

  def displayFixPoint           = verbosity > Verbosity.Normal
  def displayProgress           = verbosity > Verbosity.Normal

  def immediateReport           = debugMode
  def displayFullProgress       = false

  var displaytypeanalyses = Seq[String]() 

  def displayTypeAnalysis(toMatch: String) = {
    displaytypeanalyses.exists(strMatch(toMatch, _))
  }

  var debugfunctions       = Seq[String]() 

  def debugFunction(toMatch: String) = {
     debugfunctions.exists(strMatch(toMatch, _))
  }

  var dumpcfgs             = Seq[String]() 

  def dumpCFG(toMatch: String) = {
    dumpcfgs.exists(strMatch(toMatch, _))
  }

  var drawpt: Option[String] = None
  var dumpptgraphs           = Seq[String]()

  def dumpPTGraph(toMatch: String) = {
    dumpptgraphs.exists(strMatch(toMatch, _))
  }

  var displaypure          = Seq[String]() 

  def displayPure(toMatch: String) = {
    displaypure.exists(strMatch(toMatch, _))
  }

  var dumpCallGraph         = false
  var dumpCallStats         = false
  var dumpClassDescendents  = false

  def displayExceptionsWarn  = false //debugMode

  var wholeCodeAnalysis     = true

  var fillHierarchy         = false
  var fillGraphs            = false
  var fillGraphsIteratively = true
  var createTables          = false

  def strMatch(haystack: String, needle: String): Boolean = {
    (haystack contains needle.replace("_", "")) || (needle == "_")
  }

  def ifVerbosity(verb: Verbosity.Value)(body: => Unit) {
    if (verbosity >= verb) body
  }

  def ifVerbose(body: => Unit)    = ifVerbosity(Verbosity.Verbose)(body)
  def ifDebug(body: => Unit)      = ifVerbosity(Verbosity.Debug)(body)
}

object Verbosity extends Enumeration {
  val Quiet      = Value("Quiet",      1)
  val Normal     = Value("Normal",     2)
  val Verbose    = Value("Verbose",    3)
  val Debug      = Value("Debug",      4)

  class VerbVal(name: String, val level: Int) extends Val(nextId, name) with Ordered[Value] {
    def compare(that: VerbVal) = level compare that.level
  }

  def Value(name: String, level: Int) = new VerbVal(name, level)
}

          
