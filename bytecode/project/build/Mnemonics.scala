import sbt._

class Mnemonics(info: ProjectInfo) extends DefaultProject(info) {
  val asm = "asm" % "asm" % "3.1"
  val specs = "org.scala-tools.testing" %% "specs" % "1.6.5" % "test"
}
