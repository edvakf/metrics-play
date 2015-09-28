resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.4")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

resolvers += Classpaths.sbtPluginReleases

// Scoverage
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.0.4")

// Send Scoverage results to coveralls
addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "1.0.0")
