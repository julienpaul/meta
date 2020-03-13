package se.lu.nateko.cp.meta.test

import org.scalatest.funspec.AnyFunSpec
import se.lu.nateko.cp.meta._
import se.lu.nateko.cp.meta.onto.Onto

class OntoTests extends AnyFunSpec{

	val onto = new Onto(TestConfig.owlOnto)

	def getClassInfo(localName: String): ClassDto = {
		val classUri = TestConfig.getOWLClass(localName).getIRI.toURI
		onto.getClassInfo(classUri)
	}

	describe("getClassInfo"){

		describe("for Station class"){
			val props = getClassInfo("Station").properties

			val expected = 18

			it(s"should find $expected properties"){
				assert(props.size === expected)
			}
		}

		describe("for SpatialCoverage class"){
			val classInfo = getClassInfo("SpatialCoverage")

			it("should find correct value restrictions for latitude"){
				val latitudeRestrictions = classInfo.properties.collect{
					case p: DataPropertyDto if p.resource.displayName == "Latitude" => p.range.restrictions
				}.flatten

				val expectedRestrs = Set(MinRestrictionDto(-90), MaxRestrictionDto(90))

				assert(latitudeRestrictions.toSet === expectedRestrs)
			}
		}
	}
}
