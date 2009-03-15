package net.virtualvoid.bytecode

import _root_.org.specs._

object BytecodeCompilerSpecs extends Specification{
  def compiledTests(compiler:net.virtualvoid.bytecode.Bytecode.ByteletCompiler){
    import Bytecode._
    import Bytecode.Implicits._
    import Bytecode.Instructions._
    
    "bipush(20)" in {
      compiler.compile(classOf[String])(_~pop~bipush(20)~invokemethod1(Integer.valueOf(_)))
        .apply("Test") must be_==(20)}
    "invokemethod1(_.length)" in {
      compiler.compile(classOf[String])(_~invokemethod1(_.length)~invokemethod1(Integer.valueOf(_)))
        .apply("Test") must be_==(4)}
    "locals + method2" in {
      compiler.compile(classOf[java.lang.String])(_ ~ withLocal{ str => _ ~ str.load ~ str.load ~ invokemethod2(_.concat(_))})
      .apply("Test") must be_==("TestTest")}
    "iadd with operations" in {
      compiler.compile(classOf[java.lang.Integer])(
        _ ~ invokemethod1(_.intValue) ~ dup
        ~ iadd
        ~ invokemethod1(Integer.valueOf(_))
      ).apply(12) must be_==(24)
    }
    "iadd" in {
      compiler.compile(classOf[java.lang.Integer])(_~invokemethod1(_.intValue)~dup~iadd~bipush(3)~iadd~invokemethod1(Integer.valueOf(_)))
      .apply(12) must be_==(27)}
    "store(_) int in locals" in {
      compiler.compile(classOf[java.lang.Integer])(_~invokemethod1(_.intValue)~dup~withLocal{i=> _ ~ i.load}~iadd~invokemethod1(Integer.valueOf(_)))
      .apply(12) must be_==(24)}
    "store(_) double in locals" in {
      compiler.compile(classOf[java.lang.Double])(_~invokemethod1(_.doubleValue)~withLocal{d=>d.load}~invokemethod1(java.lang.Double.valueOf(_)))
      .apply(12.453) must be_==(12.453)}
    "store(_) double after method2" in {
      compiler.compile(classOf[java.lang.Double])(_~invokemethod1(_.doubleValue)~ldc("test")~dup~invokemethod2(_.concat(_))~pop~withLocal{d=>d.load}~invokemethod1(java.lang.Double.valueOf(_:Double)))
      .apply(12.453) must be_==(12.453)}
    "load element with index 1 from a string array" in {
      compiler.compile(classOf[Array[String]])(_.bipush(1)~aload)
      .apply(array("That","is","a","Test")) must be_==("is")
    }
    "save string element to array and load it afterwards" in {
      compiler.compile(classOf[Array[String]])(_~dup~bipush(1)~ldc("test")~astore~bipush(1)~aload)
      .apply(array("That","is","a","Test")) must be_==("test")
    }
    "save int element to array and load it afterwards" in {
      compiler.compile(classOf[Array[Int]])(_~dup~bipush(1)~bipush(13)~astore~bipush(1)~aload~dup~iadd~invokemethod1(Integer.valueOf(_)))
      .apply(array(1,2,3,4)) must be_==(26)
    }
    "get array length" in {
      compiler.compile(classOf[Array[String]])(_~arraylength~invokemethod1(Integer.valueOf(_)))
      .apply(array("That","is","a","problem")) must be_==(4)
    }
    "isub" in {
      compiler.compile(classOf[java.lang.Integer])(_~invokemethod1(_.intValue)~bipush(3)~isub~invokemethod1(Integer.valueOf(_)))
      .apply(12) must be_==(9)
    }
    "dup_x1" in {
      compiler.compile(classOf[java.lang.Integer])(_~dup~invokemethod1(_.toString)~swap~invokemethod1(_.intValue)~dup_x1~swap~pop~iadd~invokemethod1(Integer.valueOf(_)))
      .apply(12) must be_==(24)
    }
    "create new StringBuilder" in {
      compiler.compile(classOf[java.lang.String])(_~dup~newInstance(classOf[java.lang.StringBuilder])~swap~invokemethod2(_.append(_))~swap~invokemethod2(_.append(_))~invokemethod1(_.toString))
      .apply("test") must be_==("testtest") 
    }
    "store(_) string after void method" in {
      compiler.compile(classOf[java.lang.String])(_ ~ newInstance(classOf[java.text.SimpleDateFormat]) ~ ldc("yyyy") ~ invokemethod2(_.applyPattern(_)) ~ pop_unit ~ withLocal{str=>str.load})
      .apply("test") must be_==("test")
    }
    "scala parameterless method call" in {
      compiler.compile(classOf[Option[java.lang.String]])(
        _ ~
          invokemethod1(_.isDefined) ~
          invokemethod1(java.lang.Boolean.valueOf(_))
      )
      .apply(Some("x")) must be_==(true)
    }
    "method call to superclass method" in {
      compiler.compile(classOf[java.lang.StringBuilder])(
        _ ~
          invokemethod1(_.length) ~
          invokemethod1(Integer.valueOf(_))
      )
      .apply(new java.lang.StringBuilder) must be_==(0)
    }
    "method2 call to method which accepts superclass" in {
      compiler.compile(classOf[java.lang.StringBuilder])(
        _ ~
          dup ~
          invokemethod2(_.append(_)) // accepts CharSequence
      )
      .apply(new java.lang.StringBuilder)
    }
    /*"ifeq and jmp" in {
      if (compiler != Interpreter)
      compiler.compile(classOf[java.lang.Integer])(
        // sums all integers from 0 to i
        f => {
          val start = f ~
            invokemethod1(_.intValue) ~
            local[_0,Int].store() ~ //  store(_) current i in local 0
            bipush(0) ~
            local[_1,Int].store() ~ //  store(_) sum in local 1
            target
          
          start ~
            local[_0,Int].load() ~ // load i to check if it is already 0 
            ifne(f => 
              f ~ 
                local[_0,Int].load() ~
                dup ~
                bipush(1) ~
                isub ~
                local[_0,Int].store() ~
                local[_1,Int].load() ~
                iadd ~
                local[_1,Int].store() ~
                jmp(start)
            ) ~
            local[_1,Int].load() ~
            invokemethod1(Integer.valueOf(_))
      }).apply(5) must be_==(15)
    }*/
    "ifeq2" in {
      val f = compiler.compile(classOf[java.lang.Integer])(
        _ ~ 
          invokemethod1(_.intValue) ~
          bipush(5) ~
          isub ~
          ifeq2(
            _ ~
              ldc("equals 5")
            ,_ ~
              ldc("does not equal 5")
          )
      )
      f(10) must be_==("does not equal 5")
      f(5) must be_==("equals 5")
    }
    "ifne2" in {
      val f = compiler.compile(classOf[java.lang.Integer])(
        _ ~ 
          invokemethod1(_.intValue) ~
          bipush(5) ~
          isub ~
          ifne2(
            _ ~
              ldc("does not equal 5")
            ,_ ~
              ldc("equals 5")
          )
      )
      f(10) must be_==("does not equal 5")
      f(5) must be_==("equals 5")
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
  
  def println(i:Int) = System.out.println(i)
  
  {
    import Bytecode._
    import Instructions._
    import Implicits._
    import RichOperations._
      
/*
      val func = ASMCompiler.compile(classOf[java.lang.Integer])(_ //simple[Nil**java.lang.Integer]
                                                                 ~ invokemethod1(_.intValue) 
                                                                 ~ bipush(1) 
                                                                 ~ swap
                                                                 ~ tailRecursive[Nil**Int**Int,Nil,Nil**Int,Nil]{self =>
                                                                   simple[Nil**Int**Int] ~
                                                                     //state("before check: ") ~
                                                                     dup ~
                                                                     ifeq2(
                                                                       pop,
															          //simple[Nil**Int**Int]
															          fr =>
															            ider(fr,
															            id(fr)
															            ~ dup_x1 //x,sum,x
															            ~ iadd ~ dup ~ invokemethod1(println(_)) ~ pop_unit
															            //~ state("after println")
															            ~ swap
															            ~ bipush(1) ~ isub ~ self)
															        )
                                                                 } _ 
                                                                   //~ f // 0 , x
                                                                 ~ invokemethod1(Integer.valueOf(_)))
      System.out.println(func(5))
      
          
*/
      
      import java.lang.{Iterable => jIterable}
      import java.util.{Iterator => jIterator}
      
      /**
       def foldIterator(func,it,start) =
         if (it.hasNext)
            foldIterator(func,func(it.next,start))
         else
       *    start
       */
/*      def foldIterable[R<:List,LT<:List,T,U,X,It<:jIterable[T]](func:F[R**U**T,LT**jIterator[T]]=>F[R**U,LT**jIterator[T]],eleType:Class[T])
          :F[R**U**It,LT**X] => F[R**U,LT**jIterator[T]] =
        _ ~
        invokemethod1(_.iterator) ~
        local[_0,jIterator[T]].store() ~
        tailRecursive[R**U,LT**jIterator[T],R**U,LT**jIterator[T]]{ self =>
          _ ~
          local[_0,jIterator[T]].load() ~
          invokemethod1(_.hasNext) ~
          ifeq2(
                f=>f,
                _ ~
                local[_0,jIterator[T]].load() ~
                invokemethod1(_.next) ~
                checkcast(eleType) ~
                func ~
                self)         
        }
        
      
      val func2 = ASMCompiler.compile(classOf[Array[Int]])(
        _ ~
        bipush(0) ~
        dup ~
        local[_0,Int].store() ~
        foldArray(iadd) ~
        invokemethod1(Integer.valueOf(_))
      )
      
      val func3 = ASMCompiler.compile(classOf[java.util.List[java.lang.Integer]])(
        _ ~
        bipush(0) ~
        dup ~
        local[_0,Int].store() ~
        swap ~
        foldIterable[Nil,Nil,java.lang.Integer,Int,Int,java.util.List[java.lang.Integer]](
          (f:F[Nil**Int**java.lang.Integer,Nil**jIterator[java.lang.Integer]]) 
              => f ~ invokemethod1((_:java.lang.Integer).intValue) ~ iadd,classOf[java.lang.Integer]) ~
        invokemethod1(Integer.valueOf(_))
      )
      
      type SF[T<:List,U<:List,L<:List] = F[T,L] => F[U,L] 
      
      trait StackFunc1[T,U]{
        def f[R<:List,L<:List]:SF[R**T,R**U,L]
      }
      def sf[T,U](func:F[Nil**T,Nil] => F[Nil**U,Nil]):StackFunc1[T,U] =
        new StackFunc1[T,U]{
          def f[R<:List,L<:List]:SF[R**T,R**U,L] = func.asInstanceOf[SF[R**T,R**U,L]]
        }
      
      val toString = sf[java.lang.Integer,String](invokemethod1(_.toString))
      
      val empty:F[Nil,Nil] = null
      
      val x = 
        sf[Int,String](_ ~
          invokemethod1(java.lang.Integer.valueOf(_)) ~
            toString.f)
      
      //val test:String = x       
      
      System.out.println(func2(Array(5,10,3,5,2)))
      System.out.println(func3(java.util.Arrays.asList(12,4,2,6,3,7,3)):java.lang.Integer)
      */()
  }
}

/*
Description	Resource	Path	Location	Type
type mismatch;
 found   : (net.virtualvoid.bytecode.Bytecode.F[net.virtualvoid.bytecode.Bytecode.**[net.virtualvoid.bytecode.Bytecode.**[net.virtualvoid.bytecode.Bytecode.Nil,Int],java.lang.Iterable[java.lang.Integer]],net.virtualvoid.bytecode.Bytecode.**[net.virtualvoid.bytecode.Bytecode.Nil,Int]]) => net.virtualvoid.bytecode.Bytecode.F[net.virtualvoid.bytecode.Bytecode.**[net.virtualvoid.bytecode.Bytecode.Nil,Int],net.virtualvoid.bytecode.Bytecode.**[net.virtualvoid.bytecode.Bytecode.Nil,java.util.Iterator[java.lang.Integer]]]
 required: (net.virtualvoid.bytecode.Bytecode.F[net.virtualvoid.bytecode.Bytecode.**[net.virtualvoid.bytecode.Bytecode.**[net.virtualvoid.bytecode.Bytecode.Nil,Int],java.util.List[java.lang.Integer]],net.virtualvoid.bytecode.Bytecode.**[net.virtualvoid.bytecode.Bytecode.Nil,Int]]) => ?	BytecodeCompilerSpecs.scala	bytecode/src/test/scala/net/virtualvoid/bytecode	Unknown	Scala Problem

*/

import org.specs.runner.JUnit4

class MyCompilerSpecTest extends JUnit4(BytecodeCompilerSpecs)