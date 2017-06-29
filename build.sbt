val defaultScala = "2.12.2"

lazy val commonSettings = Seq(
	organization := "se.lu.nateko.cp",
	scalaVersion := defaultScala,

	scalacOptions ++= Seq(
		"-target:jvm-1.8",
		"-encoding", "UTF-8",
		"-unchecked",
		"-feature",
		"-deprecation",
		"-Xfuture",
		"-Yno-adapted-args",
		"-Ywarn-dead-code",
		"-Ywarn-numeric-widen",
		"-Ywarn-unused"
	)
)

lazy val metaCore = (project in file("core"))
	.settings(commonSettings: _*)
	.settings(
		name := "meta-core",
		version := "0.3.2-SNAPSHOT",
		libraryDependencies ++= Seq(
			"io.spray" %% "spray-json" % "1.3.2"
		),
		publishTo := {
			val nexus = "https://repo.icos-cp.eu/content/repositories/"
			if (isSnapshot.value)
				Some("snapshots" at nexus + "snapshots")
			else
				Some("releases"  at nexus + "releases")
		},
		crossScalaVersions := Seq(defaultScala, "2.11.11"),
		credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
	)

val akkaVersion = "2.4.18"
val akkaHttpVersion = "10.0.6"
val rdf4jVersion = "2.2.2"

val noGeronimo = ExclusionRule(organization = "org.apache.geronimo.specs")

lazy val meta = (project in file("."))
	.dependsOn(metaCore)
	.enablePlugins(SbtTwirl)
	.settings(commonSettings: _*)
	.settings(
		name := "meta",
		version := "0.3.0",

		libraryDependencies ++= Seq(
			"com.typesafe.akka"     %% "akka-http-spray-json"               % akkaHttpVersion,
			"com.typesafe.akka"     %% "akka-slf4j"                         % akkaVersion,
			"ch.qos.logback"         % "logback-classic"                    % "1.1.3",
			"org.eclipse.rdf4j"      % "rdf4j-repository-sail"              % rdf4jVersion,
			"org.eclipse.rdf4j"      % "rdf4j-sail-memory"                  % rdf4jVersion,
			"org.eclipse.rdf4j"      % "rdf4j-rio-rdfxml"                   % rdf4jVersion,
			"org.eclipse.rdf4j"      % "rdf4j-queryresultio-sparqljson"     % rdf4jVersion,
			"org.eclipse.rdf4j"      % "rdf4j-queryresultio-text"           % rdf4jVersion,
			"org.eclipse.rdf4j"      % "rdf4j-queryalgebra-geosparql"       % rdf4jVersion,
			"org.postgresql"         % "postgresql"                         % "9.4-1201-jdbc41",
			"net.sourceforge.owlapi" % "org.semanticweb.hermit"             % "1.3.8.510" excludeAll(noGeronimo),
			"org.apache.commons"     % "commons-email"                      % "1.4",
			"se.lu.nateko.cp"       %% "views-core"                         % "0.2-SNAPSHOT",
			"se.lu.nateko.cp"       %% "cpauth-core"                        % "0.5-SNAPSHOT",
			"org.scalatest"         %% "scalatest"                          % "3.0.1" % "test"
		),

		scalacOptions += "-Ywarn-unused-import:false",

		assemblyMergeStrategy in assembly := {
			case PathList("META-INF", "axiom.xml") => MergeStrategy.first
			case PathList("META-INF", "maven", "com.google.guava", "guava", "pom.properties") => MergeStrategy.first
			case PathList("META-INF", "maven", "com.google.guava", "guava", "pom.xml") => MergeStrategy.first
			case PathList("org", "apache", "commons", "logging", _*) => MergeStrategy.first
			case "application.conf" => MergeStrategy.concat
			case x => ((assemblyMergeStrategy in assembly).value)(x)
			//case PathList(ps @ _*) if(ps.exists(_.contains("guava")) && ps.last == "pom.xml") => {println(ps); MergeStrategy.first}
		},

		initialCommands in console in Test := """
			import se.lu.nateko.cp.meta.test.Playground._
		""",

		cleanupCommands in console in Test := """
			stop()
		"""
	)

/*
lazy val jobAd = (project in file("jobAd"))
	.settings(commonSettings: _*)
	.enablePlugins(SbtTwirl)
	.settings(
		name := "jobAd",
		version := "1.0",
		libraryDependencies ++= Seq(
			"com.typesafe.akka"     %% "akka-http-spray-json-experimental"  % akkaVersion,
			"com.typesafe.akka"     %% "akka-slf4j"                         % akkaVersion,
			"com.fasterxml.uuid"     % "java-uuid-generator"                % "3.1.4",
			"ch.qos.logback"         % "logback-classic"                    % "1.1.3",
			"se.lu.nateko.cp"       %% "views-core"                         % "0.1-SNAPSHOT"
		)
	)
*/
