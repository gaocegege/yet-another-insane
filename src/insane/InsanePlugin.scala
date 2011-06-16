package insane

import utils.Reporters.Reporter
import utils.Settings
import utils.Verbosity
import utils.XMLConfig

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin,PluginComponent}

class InsanePlugin(val global: Global) extends Plugin {

  val name = "insane"
  val description = "INterprocedural Shape ANalysis Engine"

  val settings = new Settings()

  val reporter = new Reporter(global, settings)

  /** The help message displaying the options for that plugin. */
  override val optionsHelp: Option[String] = Some(
    "  --dumpcfg=s1:s2        Dumps CFG for the given symbols, _ for all" + "\n" +
    "  --dumppt=s1:s2         Dumps Point-to graphs for the given symbols, _ for all" + "\n" +
    "  --dumphierarchy        Dumps class hierarchy graph" + "\n" +
    "  --debugfun=s1:s2       Debug given function symbols" + "\n" +
    "  --dumpcallgraph        Dumps call graph resulting of class analysis" + "\n" +
    "  --verbosity=normal     Sets verbosity (quiet < normal < verbose < debug)" + "\n" +
    "  --verbose              Sets verbosity to verbose" + "\n" +
    "  --quiet                Sets verbosity to quiet" + "\n" +
    "  --debug                Sets verbosity to debug" + "\n" +
    "  --displayta=s1:s2      Displays Type Analysis results for the given symbols, _ for all" + "\n" +
    "  --displaypure=s1:s2    Displays Purity info for the given symbols, _ for all"
  )

  /** Processes the command-line options. */
  private def splitList(lst: String) : Seq[String] = lst.split(':').map(_.trim).filter(!_.isEmpty)

  override def processOptions(options: List[String], error: String => Unit) {
    var setVerbosity = false

    for(option <- options) {
      option.split("=", 2).toList match {
        case "config"   :: path :: Nil  =>
          new XMLConfig(path).load(settings)

        case "dumpcfg"   :: symbols :: Nil  =>
          settings.dumpcfgs = splitList(symbols)

        case "dumppt"   :: symbols :: Nil  =>
          settings.dumpptgraphs = splitList(symbols)

        case "debugfun"   :: symbols :: Nil  =>
          settings.dumpptgraphs     = splitList(symbols)
          settings.dumpcfgs         = splitList(symbols)
          settings.debugfunctions   = splitList(symbols)

        case "displaypure"   :: symbols :: Nil  =>
          settings.displaypure = splitList(symbols)

        case "dumphierarchy" :: Nil  =>
          settings.dumpClassDescendents = true
        case "dumpcallgraph" :: Nil  =>
          settings.dumpCallGraph = true

        case "verbosity" :: verb :: Nil     =>
          if (setVerbosity) {
            error("Can't set verbosity twice")
          }
          verb.toLowerCase match {
            case "quiet"   => settings.verbosity = Verbosity.Quiet
            case "normal"  => settings.verbosity = Verbosity.Normal
            case "verbose" => settings.verbosity = Verbosity.Verbose
            case "debug"   => settings.verbosity = Verbosity.Debug
            case _         => error("Invalid verbosity: "+verb)
          }
          setVerbosity = true

        case "verbose" :: Nil  =>
          if (setVerbosity) {
            error("Can't set verbosity twice")
          }
          settings.verbosity = Verbosity.Verbose
          setVerbosity = true

        case "createtables" :: Nil  =>
          settings.createTables = true

        case "buildhierarchy" :: Nil  =>
          settings.buildHierarchy = true

        case "buildgraphs" :: Nil  =>
          settings.buildGraphs = true

        case "quiet" :: Nil  =>
          if (setVerbosity) {
            error("Can't set verbosity twice")
          }
          settings.verbosity = Verbosity.Verbose
          setVerbosity = true

        case "debug" :: Nil  =>
          if (setVerbosity) {
            error("Can't set verbosity twice")
          }
          settings.verbosity = Verbosity.Debug
          setVerbosity = true

        case "displayta"   :: symbols :: Nil   =>
          settings.displaytypeanalyses = splitList(symbols)

        case _                              => error("Invalid option: " + option)
      }
    }
  }

  val analysisComponent  = new AnalysisComponent(this, reporter, settings) {
    val global: InsanePlugin.this.global.type = InsanePlugin.this.global
  }

  val components = List[PluginComponent](analysisComponent)
}
