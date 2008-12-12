package net.virtualvoid.bytecode

import _root_.org.specs._

object BytecodeCompilerSpecs extends Specification{
  def compiledTests(compiler:net.virtualvoid.bytecode.Bytecode.ByteletCompiler){
    import Bytecode._
    import Bytecode.Implicits._
    import Bytecode.Operations._
    
    "bipush(20)" in {
      compiler.compile(classOf[String])(_~pop~bipush(20)~method(Integer.valueOf(_)))
        .apply("Test") must be_==(20)}
    "method(_.length)" in {
      compiler.compile(classOf[String])(_~method(_.length)~method(Integer.valueOf(_)))
        .apply("Test") must be_==(4)}
    "locals + method2" in {
      compiler.compile(classOf[java.lang.String])(_~(_.l.store.e)~load(l0)~load(l0)~method2(_.concat(_)))
      .apply("Test") must be_==("TestTest")}
    "iadd with operations" in {
      compiler.compile(classOf[java.lang.Integer])(
        _ ~ method(_.intValue) ~ dup
        ~ iadd
        ~ method(Integer.valueOf(_))
      ).apply(12) must be_==(24)
    }
    "iadd" in {
      compiler.compile(classOf[java.lang.Integer])(_~method(_.intValue)~dup~iadd~bipush(3)~iadd~method(Integer.valueOf(_)))
      .apply(12) must be_==(27)}
    "store int in locals" in {
      compiler.compile(classOf[java.lang.Integer])(_~method(_.intValue)~dup~(_.l.store.e)~load(l0)~iadd~method(Integer.valueOf(_)))
      .apply(12) must be_==(24)}
    "store double in locals" in {
      compiler.compile(classOf[java.lang.Double])(_~method(_.doubleValue)~(_.l.store.e)~load(l0)~method(java.lang.Double.valueOf(_)))
      .apply(12.453) must be_==(12.453)}
    "store double after method2" in {
      compiler.compile(classOf[java.lang.Double])(_~method(_.doubleValue)~ldc("test")~dup~method2(_.concat(_))~pop~(_.l.store.e)~load(l0)~method(java.lang.Double.valueOf(_:Double)))
      .apply(12.453) must be_==(12.453)}
    "store something more than 1 level deep" in {
      compiler.compile(classOf[String])(_.l.l.store.e.e ~ load(l1))
      .apply("test") must be_==("test")
    }
    "load element with index 1 from a string array" in {
      compiler.compile(classOf[Array[String]])(_.bipush(1)~aload)
      .apply(array("That","is","a","Test")) must be_==("is")
    }
    "save string element to array and load it afterwards" in {
      compiler.compile(classOf[Array[String]])(_~dup~bipush(1)~ldc("test")~astore~bipush(1)~aload)
      .apply(array("That","is","a","Test")) must be_==("test")
    }
    "save int element to array and load it afterwards" in {
      compiler.compile(classOf[Array[Int]])(_~dup~bipush(1)~bipush(13)~astore~bipush(1)~aload~dup~iadd~method(Integer.valueOf(_)))
      .apply(array(1,2,3,4)) must be_==(26)
    }
    "get array length" in {
      compiler.compile(classOf[Array[String]])(_~arraylength~method(Integer.valueOf(_)))
      .apply(array("That","is","a","problem")) must be_==(4)
    }
    "isub" in {
      compiler.compile(classOf[java.lang.Integer])(_~method(_.intValue)~bipush(3)~isub~method(Integer.valueOf(_)))
      .apply(12) must be_==(9)
    }
    "dup_x1" in {
      compiler.compile(classOf[java.lang.Integer])(_~dup~method(_.toString)~swap~method(_.intValue)~dup_x1~swap~pop~iadd~method(Integer.valueOf(_)))
      .apply(12) must be_==(24)
    }
    "create new StringBuilder" in {
      compiler.compile(classOf[java.lang.String])(_~dup~newInstance(classOf[java.lang.StringBuilder])~swap~method2(_.append(_))~swap~method2(_.append(_))~method(_.toString))
      .apply("test") must be_==("testtest") 
    }
    "store string after void method" in {
      compiler.compile(classOf[java.lang.String])(_ ~ newInstance(classOf[java.text.SimpleDateFormat]) ~ ldc("yyyy") ~ method2(_.applyPattern(_)) ~ pop_unit ~ (_.l.store.e) ~ load(l0))
      .apply("test") must be_==("test")
    }
    "ifeq and jmp" in {
      compiler.compile(classOf[java.lang.Integer])(
        f => {
          val start = f ~
            method(_.intValue) ~
            (_.l.store.e) ~
            bipush(0) ~
            (_.l.l.store.e.e) ~
            target
          
          start ~
            load(l0) ~
            ifeq(f => 
              f ~ load(l0) ~
                dup ~
                bipush(1) ~
                isub ~
                (_.l.store.e) ~
                load(l1) ~
                iadd ~
                (_.l.l.store.e.e) ~
                jmp(start)
            ) ~
            load(l1) ~
            method(Integer.valueOf(_))
      }).apply(5) must be_==(15)
    }
  }
  def array(els:Int*):Array[Int] = Array(els:_*)
  def array(els:String*):Array[String] = Array(els:_*)
  
  "Compiler" should {
    "succeed in generic Tests" in compiledTests(net.virtualvoid.bytecode.ASMCompiler)
  }
  "Interpreter" should {
    "succeed in generic Tests" in compiledTests(net.virtualvoid.bytecode.Interpreter)
  }
  
  {
    import Bytecode._
    import Operations._
    import Implicits._
      def ifeq2[R<:List,LT<:List,ST2<:List,LT2<:List](then:F[R,LT]=>F[ST2,LT2],elseB:F[R,LT]=>F[ST2,LT2]):F[R**Int,LT]=>F[ST2,LT2] = f=>f.ifeq2_int[R,ST2,LT2](f.stack.rest,f.stack.top,then,elseB)
      def call[ST1<:List,ST2<:List,LT1<:List,LT2<:List](f: () => (F[ST1,LT1]=>F[ST2,LT2])):F[ST1,LT1]=>F[ST2,LT2] = f()
      
      def simple[R<:List]:F[R,Nil]=>F[R,Nil] = f => f
      
      lazy val f:F[Nil**Int**Int,Nil]=>F[Nil**Int,Nil] =
        simple[Nil**Int**Int] ~
        dup ~
        ifeq2(
          pop,
          simple[Nil**Int**Int] 
          ~ dup_x1 
            ~ swap 
            ~ iadd ~ dup ~ method(System.out.println(_)) ~ pop_unit 
            ~ swap 
            ~ bipush(1) ~ isub ~ call(() => f)
          
        )
      
      val func = Interpreter.compile(classOf[java.lang.Integer])(simple[Nil**java.lang.Integer]
                                                                 ~ method(_.intValue) 
                                                                 ~ bipush(0) 
                                                                 ~ swap 
                                                                   ~ f 
                                                                   ~ method(Integer.valueOf(_)))
      
      System.out.println(func(5))
      System.out.println(func(10))
      }
}

import org.specs.runner.JUnit4

class MyCompilerSpecTest extends JUnit4(BytecodeCompilerSpecs)