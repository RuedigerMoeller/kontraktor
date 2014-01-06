import sbt._

object Plugins extends Build {
  lazy val root = Project("plugins", file(".")) dependsOn( g8plugin )
  lazy val g8plugin = ProjectRef(uri("git://github.com/n8han/giter8#0.4.5"), "giter8-plugin")
}
