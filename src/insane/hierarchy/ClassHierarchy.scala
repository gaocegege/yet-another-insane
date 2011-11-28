package insane
package hierarchy

import utils.Graphs._
import utils._
import storage.Database
import utils.Reporters.{CompilerReporterPassThrough,posToOptPos}
import collection.mutable.Queue

trait ClassHierarchy { self: AnalysisComponent =>

  import global._

  class ClassHierarchyPhase extends SubPhase {
    val name = "Generating/Loading class hierarchy"

    def loadFromClassfiles() {
      import collection.mutable.Set

      // We traverse the symbols, for previously compiled symbols
      val oldReporter = global.reporter

      global.reporter = CompilerReporterPassThrough( (msg, pos) => settings.ifVerbose( reporter.warn(msg, pos.asInstanceOf[tools.nsc.util.Position]) ))

      var seen  = Set[Symbol]()
      var lastSeen = seen;
      var i = 0;
      do {
        i += 1;

        lastSeen = seen
        seen = Set()

        var queue = Queue[Symbol](definitions.RootClass)
        while (!queue.isEmpty) {
          val sym = queue.dequeue
          if (sym.isClass || sym.isModule || sym.isTrait || sym.isPackage) {
            if (sym.rawInfo.isComplete || !safeFullName(sym).contains("$")) {
              val tpesym = if (sym.isType) sym else sym.tpe.typeSymbol

              if (!(seen contains tpesym) && tpesym != NoSymbol) {
                seen += tpesym

                if (!sym.isPackage) {
                  classHierarchyGraph.addSingleNode(tpesym)
                  for (parentType <- tpesym.info.parents) {
                    val parent = parentType.typeSymbol
                    if (parent != NoSymbol) {
                      classHierarchyGraph.addEdge(parent, tpesym)
                    } else {
                      reporter.debug("Woops, "+parentType+" has no typesymbol == NoSymbol");
                    }
                  }
                }


                queue ++= tpesym.tpe.members
              }
            }
          } else if (!sym.isMethod && !sym.isValue) {
            reporter.warn("Ignored "+sym, sym.pos)
          }
        }
      } while(lastSeen != seen)

      reporter.msg("Loaded "+seen.size+" symbols in "+i+" descents")

      global.reporter = oldReporter

    }

    def loadFromTrees() {
      def traverseStep(tree: Tree) = tree match {
        case cd @ ClassDef(modes, name, tparams, impl) =>
          val classSymbol = cd.symbol

          assert(classSymbol.isType, "Class symbol "+uniqueClassName(classSymbol)+" is not a type!")

          classHierarchyGraph.addSingleNode(classSymbol)
          for (parentType <- classSymbol.info.parents) {
            val parent = parentType.typeSymbol
            if (parent != NoSymbol) {
              classHierarchyGraph.addEdge(parent, classSymbol)
            } else {
              reporter.debug("Woops, "+parentType+" has no typesymbol == NoSymbol");
            }
          }
        case _ =>
      }
      for (unit <- currentRun.units) {
        new ForeachTreeTraverser(traverseStep).traverse(unit.body)
      }
    }

    def run() {
      if (settings.fillHierarchy) {
        loadFromClassfiles()
        fillDatabase()
      } else {
        loadFromTrees()
      }

      if (settings.dumpClassDescendents) {
        val path = "classgraph.dot";
        reporter.msg("Dumping Class Graph to "+path)
        new DotConverter(classHierarchyGraph, "Class Graph").writeFile(path)
      }
    }

    def fillDatabase() {
      if (Database.active) {
        def fixChain(s: Symbol) {
          if (s != definitions.ObjectClass) {
            val parent = if (s.superClass == NoSymbol) definitions.ObjectClass else s.superClass

            classHierarchyGraph.addEdge(parent, s)

            fixChain(parent)
          }
        }

        var roots = classHierarchyGraph.V &~ classHierarchyGraph.V.flatMap(v => classHierarchyGraph.outEdges(v).map(_.v2))

        for (r <- roots) {
          fixChain(r.symbol)
        }

        reporter.info("Inserting "+classHierarchyGraph.V.size+" hierarchy entries in the database...")

        var toInsert = Set[(String, Long, Long)]()

        def insert(v: CHVertex, left: Long): Long = {
          val sym = v.symbol

          var currentLeft = left + 1

          for (CDEdge(_, v2) <- classHierarchyGraph.outEdges(v)) {
            currentLeft = insert(v2, currentLeft)
          }

          toInsert += ((uniqueClassName(sym), left, currentLeft))

          currentLeft + 1
        }

        insert(classHierarchyGraph.sToV(definitions.ObjectClass), 1)

        Database.Hierarchy.insertAll(toInsert)
      } else {
        reporter.error("Cannot insert into database: No database configuration")
      }
    }
  }

  case class CHVertex(symbol: Symbol) extends VertexAbs {
    val name = symbol.name.toString()
    var children = Set[CHVertex]()
  }

  case class CDEdge(v1: CHVertex, v2: CHVertex) extends EdgeAbs[CHVertex]

  class ClassHierarchyGraph extends MutableDirectedGraphImp[CHVertex, CDEdge] {
    var sToV = Map[Symbol, CHVertex]()

    def addEdge(parent: Symbol, child: Symbol) = {
      if (!sToV.contains(parent)) {
        sToV += parent -> CHVertex(parent)
      }

      if (!sToV.contains(child)) {
        sToV += child -> CHVertex(child)
      }

      val vParent = sToV(parent)
      val vChild  = sToV(child)

      this += CDEdge(vParent, vChild)
      vParent.children += vChild
    }

    def addSingleNode(node: Symbol) = {
      if (!sToV.contains(node)) {
        sToV += node -> CHVertex(node)
        this += sToV(node)
      }
    }

  }
  def debugSymbol(sym: Symbol) {
    println(Console.CYAN+"Symbol:"+Console.RESET+" "+sym+" (ID: "+sym.id+")") 
    if (sym == NoSymbol) return;
    val isComplete = sym.rawInfo.isComplete
    //println("  owner:         "+sym.owner+" (ID: "+sym.owner.id+")")
    //println("  cont. in own.: "+(sym.owner.tpe.members contains sym))
    //println("  isComplete:    "+isComplete)
    //println("  isClass:       "+sym.isClass)
    val comp = if(sym.isModuleClass) sym.companionModule else sym.companionClass
    //println("  companion:     "+comp+" (ID: "+comp.id+")")
    println("  isModule:      "+sym.isModule)
    println("  isModuleClass: "+sym.isModuleClass)
    println("  isTrait:       "+sym.isTrait)
    println("  isfinal:       "+sym.isFinal)
    println("  isPackage:     "+sym.isPackage)
    println("  isMethod:      "+sym.isMethod)
    println("  isValue:       "+sym.isValue)

    if (isComplete) {
      val tpesym = if (sym.isType) sym else sym.tpe.typeSymbol
     // println("  isType:        "+sym.isType)
     // println("  sym==type:     "+(sym == tpesym))
      println("  Type:          "+tpesym)
      println("  TypeAncestors: "+tpesym.ancestors.mkString(", "))
      println("  parents:       ");
        for (t <- tpesym.info.parents) {
          val s = t.typeSymbol
          val v = classHierarchyGraph.sToV(s);
          println("                 "+t+"("+s+")");            
          println("                   - parents:   "+classHierarchyGraph.inEdges(v).size)
          println("                   - childs:    "+classHierarchyGraph.outEdges(v).size)
        }
      println("  Superclass:    "+tpesym.superClass)

      if (classHierarchyGraph.sToV.contains(tpesym)) {
        val v = classHierarchyGraph.sToV(tpesym);
        println("  In HGraph:    yes")
        println("   - parents:   "+classHierarchyGraph.inEdges(v).size)
        println("   - childs:    "+classHierarchyGraph.outEdges(v).size)
      } else {
        println("  In HGraph:    no")
      }
    }
  }

  def symbolInfo(sym: Symbol) = {
    List(
      if(sym.isClass) "Cl" else "",
      if(sym.rawInfo.isComplete) "Co" else "",
      if(sym.isModule) "Mo" else "",
      if(sym.isModuleClass) "Mc" else "",
      if(sym.isPackage) "P" else ""
    ).mkString(",")
  }

  var descendentsCache          = Map[Symbol, Set[Symbol]]()
  var directDescendentsCache    = Map[Symbol, Set[Symbol]]()

  def lookupClassSymbol(str: String): Option[Symbol] = {
    val ds = ClassSymbolUnSerializer(str).unserialize()

    if (ds == NoSymbol) {
      None
    } else {
      Some(ds)
    }
  }

  def getDirectDescendents(s: Symbol): Set[Symbol] = {
    val tpesym = if (s.isType) s else s.tpe.typeSymbol
    val tpe    = s.tpe
    if (!tpesym.isClass) {
      Set[Symbol]()
    } else {
      if (!directDescendentsCache.contains(tpesym)) {
        val set = if (tpesym.isFinal) {
          Set[Symbol]()
        } else if (classHierarchyGraph.sToV contains tpesym) {
          classHierarchyGraph.sToV(tpesym).children.map(_.symbol)
        } else if (Database.active) {
          sys.error("Not implemented yet!");
          Set[Symbol]()
        } else {
          Set[Symbol]()
        }
        directDescendentsCache += tpesym -> set
        set
      } else {
        directDescendentsCache(tpesym)
      }
    }
  }

  def getDescendents(s: Symbol): Set[Symbol] = {
    val tpesym = if (s.isType) s else s.tpe.typeSymbol
    val tpe    = s.tpe

    if (!tpesym.isClass) {
        settings.ifDebug {
          reporter.warn("Symbol "+safeFullName(tpesym)+" is not a class!");
          debugSymbol(tpesym)
        }
      Set[Symbol]()
    } else {
      if (!descendentsCache.contains(tpesym)) {
        val set = if (tpesym.isFinal) {
          Set[Symbol]()
        } else if (classHierarchyGraph.sToV contains tpesym) {
          classHierarchyGraph.sToV(tpesym).children.flatMap(v => getDescendents(v.symbol)+v.symbol)
        } else if (Database.active) {
          // We request the database
          val name    = uniqueClassName(tpesym)
          val subTree = Database.Hierarchy.subTree(name).flatMap(lookupClassSymbol _)

          if (subTree.isEmpty && Database.Hierarchy.transLookup(name).isEmpty) {
            reporter.warn("Unable to obtain descendents of unvisited type: "+tpesym, Some(tpesym.pos))
            debugSymbol(tpesym)
          }

          subTree
        } else {
          settings.ifDebug {
            if (safeFullName(tpesym).startsWith("scala.")) {
              reporter.warn("Symbol "+safeFullName(tpesym)+" not found in the class hierarchy graph!");
            }
          }
          Set[Symbol]()
        }
        descendentsCache += tpesym -> set
        set
      } else {
        descendentsCache(tpesym)
      }

    }
  }

}
