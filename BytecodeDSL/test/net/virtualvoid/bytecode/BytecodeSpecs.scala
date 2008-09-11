package net.virtualvoid.bytecode

import org.specs._

import scala.tools.nsc.{Interpreter,Settings}

object BytecodeSpecs extends Specification {
  import scala.tools.nsc.reporters._

  val mySettings = new Settings
  object interpreter extends Interpreter(mySettings){
    var writer = new java.io.StringWriter
    var pWriter = newWriter
    def newWriter = new java.io.PrintWriter(writer)
    
    def lastError = writer.toString
    
    object myReporter extends ConsoleReporter(mySettings,null,null){
      override def printMessage(msg: String) { pWriter.print(msg + "\n"); pWriter.flush() }
      override def reset{
        writer = new java.io.StringWriter
        pWriter = newWriter
        super.reset
      }
    }
    override def newCompiler(se:Settings,reporter:Reporter) = {
      super.newCompiler(se,myReporter)
    }
  }
  
  interpreter.interpret("import net.virtualvoid.bytecode.v2.Bytecode._")
  interpreter.interpret("import net.virtualvoid.bytecode.v2.Bytecode.Implicits._")
  
  import org.specs.matcher.Matcher
  def compilePrefixed(prefix:String,suffix:String) = new Matcher[String]{
    def apply(str: =>String) = 
      {
        interpreter.myReporter.reset
        interpreter.compileString(
          """object Test {
import net.virtualvoid.bytecode.v2.Bytecode._
import net.virtualvoid.bytecode.v2.Bytecode.Implicits._
"""+prefix+str+suffix+"}")
        (!interpreter.myReporter.hasErrors,"compiled","did not compile with error: "+interpreter.lastError)
      }      
  }
  def compile = compilePrefixed("","")
  def compileWithStack(stack:String) = compilePrefixed("(null:F["+stack+",Nil]).","")
  
  case class Frame(stack:String,locals:String){
    def state:String = stack + "," + locals
  }
  case class Stack(s:String) extends Frame(s,"Nil")
  case class Locals(l:String) extends Frame("Nil",l)
  
  def haveOp(op:String) = new Matcher[Frame]{
    val inner = compilePrefixed("(null:F[","])."+op)
    def apply(f: =>Frame) = inner(f.state) 
  }  

  import org.specs.specification.Example
  def suffix(suffix:String)(e: =>Example) = {currentSut.verb += suffix;e}
  def apply = suffix(" apply")(_)
  def notApply = suffix(" not apply")(_)
  
  "implicits" should apply {
    "dup on Int Stack" in {Stack("Nil**Int") must haveOp("dup")}
    
    "iadd on Int**Int" in {Stack("Nil**Int**Int") must haveOp("iadd")}
    "iadd on _**Int**Int" in {Stack("(_<:List)**Int**Int") must haveOp("iadd")}
   
    "l.load.e.dup.iadd with Int local" in {Locals("Nil**Int") must haveOp("l.load.e.dup.iadd")}
    "l.store.e on no locals (should generate one local)" in {Frame("Nil**String","Nil") must haveOp("l.store.e.l.load.e.method(_.length)")}
  }
  
  "implicits" should notApply {
    "dup on empty Stack" in {Stack("Nil") mustNot haveOp("dup")}
    "pop on empty Stack" in {Stack("Nil") mustNot haveOp("pop")}
    
    "iadd on String**Int" in {Stack("Nil**String**Int") mustNot haveOp("iadd")}
  }
}
