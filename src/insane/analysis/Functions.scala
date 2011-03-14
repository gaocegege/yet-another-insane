package insane
package analysis

import scala.tools.nsc._
import CFG.ControlFlowGraph

trait Functions {
  self : AnalysisComponent =>
  val global: Global

  import global._
  import global.definitions._

  sealed abstract class AbsFunction {
    val symbol: Symbol
    val body: Tree
    var cfg: Option[ControlFlowGraph[CFGTrees.Statement]] = None
    val argsargs: Seq[Seq[ValDef]]

    /* contracts */
    var contrRequires = Seq[Requires]()
    var contrEnsures  = Seq[Ensures]()
  }

  class NamedFunction(val symbol: Symbol, val name: Name, val argsargs: Seq[Seq[ValDef]], val body: Tree) extends AbsFunction {

  }

  class AnnonFunction(val symbol: Symbol, val args: Seq[ValDef], val body: Tree) extends AbsFunction {
    val argsargs = Seq(args)

  }

  sealed abstract class AbsCallTarget {
    val isExhaustive: Boolean
    val targets: Set[Symbol]
  }

  class OpenCallTargets(val targets: Set[Symbol]) extends AbsCallTarget {
    val isExhaustive = false;
  }

  class ClosedCallTargets(val targets: Set[Symbol]) extends AbsCallTarget {
    val isExhaustive = true;
  }
}
