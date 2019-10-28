package se.lu.nateko.cp.meta.services.sparql.magic.fusion

import scala.collection.JavaConverters._
import se.lu.nateko.cp.meta.services.CpmetaVocab
import PatternFinder._
import org.eclipse.rdf4j.query.algebra.Group
import org.eclipse.rdf4j.query.algebra.Count
import org.eclipse.rdf4j.query.algebra.Var
import org.eclipse.rdf4j.model.IRI
import se.lu.nateko.cp.meta.services.sparql.index.DataObjectFetch.Filtering
import org.eclipse.rdf4j.query.algebra.Extension
import org.eclipse.rdf4j.query.algebra.ValueExpr

class StatsFetchPatternSearch(meta: CpmetaVocab){
	import StatsFetchPatternSearch._
	import StatementPatternSearch._

	private val dofps = new DataObjectFetchPatternSearch(meta)

	val search: TopNodeSearch[StatsFetchPattern] = takeNode
		.ifIs[Extension]
		.thenSearch{ext =>
			for(
				(countVar, dobjVar) <- singleCountExtension(ext);
				grPatt <- groupSearch(dobjVar)(ext)
			) yield new StatsFetchPattern(
				ext,
				new StatsFetchNode(countVar, grPatt)
			)
		}
		.recursive

	def groupSearch(dobjVar: String): TopNodeSearch[GroupPattern] = takeNode
		.ifIs[Group]
		.recursive
		.thenSearch{g =>
			for(
				countVar <- singleVarCountGroup(g) if countVar == dobjVar;
				submitterVar <- provSearch(countVar, meta.wasSubmittedBy)(g);
				stationVar <- provSearch(countVar, meta.wasAcquiredBy)(g);
				specVar <- specSearch(countVar)(g)
				if g.getGroupBindingNames.asScala == Set(specVar, submitterVar, stationVar);
				filtering = filterSearch(countVar, g)
			) yield new GroupPattern(filtering, submitterVar, stationVar, specVar)
		}

	def provSearch(dobjVar: String, provPred: IRI): TopNodeSearch[String] = twoStepPropPath(provPred, meta.prov.wasAssociatedWith)
		.filter(_.subjVariable == dobjVar)
		.thenGet(_.objVariable)

	def specSearch(dobjVar: String): TopNodeSearch[String] = byPredicate(meta.hasObjectSpec)
		.recursive
		.thenSearch(nonAnonymous)
		.filter(_.subjVar == dobjVar)
		.thenGet(_.objVar)

	def noDeprecationSearch(dobjVar: String): TopNodeSearch[Unit] = dofps.isLatestDobjVersionFilter
		.filter(_.dobjVar == dobjVar)
		.thenGet(_ => ())

	def filterSearch(dobjVar: String, node: Group): Filtering = {
		val noDepr = noDeprecationSearch(dobjVar)(node).isDefined
		val contPatts = dofps.continuousVarPatterns(node).collect{
			case (`dobjVar`, patt) => patt
		}
		val filters = dofps.filterSearch(contPatts).search(node).map(_.filters).getOrElse(Nil)
		new Filtering(filters, noDepr, contPatts.map(_.property))
	}
}

object StatsFetchPatternSearch{

	def singleVarCountGroup(g: Group): Option[String] = g.getGroupElements().asScala match{
		case Seq(elem) => singleVarCount(elem.getOperator)
		case _         => None
	}

	def singleCountExtension(ext: Extension): Option[(String, String)] = ext.getElements().asScala match{
		case Seq(elem) => singleVarCount(elem.getExpr).map(elem.getName -> _)
		case _         => None
	}

	def singleVarCount(expr: ValueExpr): Option[String] = expr match{
		case cnt: Count =>
			cnt.getArg match {
				case v: Var if !v.isAnonymous => Some(v.getName)
				case _ => None
			}
		case _ => None
	}

	case class GroupPattern(filtering: Filtering, submitterVar: String, stationVar: String, specVar: String)

	class StatsFetchPattern(expr: Extension, statsNode: StatsFetchNode){
		def fuse(): Unit = expr.replaceWith(statsNode)
	}
}
