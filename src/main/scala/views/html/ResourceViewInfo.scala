package views.html

import se.lu.nateko.cp.meta.core.data.UriResource
import se.lu.nateko.cp.meta.core.data.Envri
import ResourceViewInfo.PropValue

case class ResourceViewInfo(
	res: UriResource,
	infrastructure: Envri.Value,
	comment: Option[String],
	types: List[UriResource],
	propValues: List[(UriResource, PropValue)],
	usage: Seq[(UriResource, UriResource)]
){
	def isEmpty: Boolean = propValues.isEmpty && usage.isEmpty && types.isEmpty && comment.isEmpty
}

object ResourceViewInfo{
	type PropValue = Either[UriResource, String]
}