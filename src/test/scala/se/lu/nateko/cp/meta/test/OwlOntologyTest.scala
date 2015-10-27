package se.lu.nateko.cp.meta.test

import org.scalatest.FunSpec
import se.lu.nateko.cp.meta.onto.Vocab
import org.semanticweb.owlapi.model.parameters.Imports


class OwlOntologyTest extends FunSpec{

	val onto = TestConfig.owlOnto

	describe("OWLOntology.isDeclared"){

		it("should distinguish between entity types even if they have same URI"){
			val realProp = TestConfig.getDataProperty("hasName")
			val fakeProp = TestConfig.getObjectProperty("hasName")

			assert(onto.isDeclared(realProp, Imports.INCLUDED))
			assert(!onto.isDeclared(fakeProp, Imports.INCLUDED))
		}
	}
}