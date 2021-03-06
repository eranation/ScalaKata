package com.scalakata

import scala.language.experimental.macros
import collection.immutable.Queue

package object eval {
  type Range = (Int, Int)
  type OrderedRender = List[(Range, List[Render])]

  sealed trait Render

  sealed trait Expression extends Render
  case class EString(v: String) extends Expression
  case class Other(repr: String) extends Expression

  case class Block(childs: OrderedRender ) extends Render
  case class Steps(simplifications: List[Expression]) extends Render

  case class Markdown(a: String) extends Render {
    override def toString = a
    def stripMargin = Markdown(a.stripMargin)
  }

  case class Markdown2(a: String) extends Render {
    override def toString = a
    def stripMargin = Markdown(a.stripMargin)
  }

  case class Html(a: String, height: Int = 500) extends Render {
    override def toString = a
    def stripMargin = Html(a.stripMargin)
  }

  case class Html2(a: String) extends Render {
    override def toString = a
    def stripMargin = Html2(a.stripMargin)
  }

  def isNotUnit(a: Any) = {
    a match {
      case _: Unit ⇒ false
      case _ ⇒ true
    }
  }
  def render[A >: Null](a: A): Render = {
  	a match {
    	case null ⇒ Other("null")
      case ar: Array[_] ⇒ Other(ar.deep.toString)
      case v: String ⇒  EString(v)
      case _: scala.xml.Elem ⇒ Html(a.toString)
      case md: Markdown ⇒ md
      case md2: Markdown2 ⇒ md2
      case h: Html ⇒ h
      case h2: Html2 ⇒ h2
      case other ⇒ Other(other.toString)
    }
  }

  def htmlFile(clazz: Class[_], filename: String, size: Int = 500): Html =
    Option(clazz.getClassLoader.getResourceAsStream(filename)).map{ res ⇒
      Html(io.Source.fromInputStream(res).mkString, size)
    }.getOrElse(Html(s"<h1>$filename not found</h1>", size))

  implicit class MarkdownHelper(val sc: StringContext) extends AnyVal {
    def markdown(args: Any*): Markdown = {
      Markdown(sc.s(args: _*))
    }
    def md(args: Any*) = markdown(args: _*)
    def markdownR(args: Any*): Markdown = {
      Markdown(sc.raw(args: _*))
    }
    def mdR(args: Any*) = markdownR(args: _*)
  }
  implicit class MarkdownHelper2(val sc: StringContext) extends AnyVal {
    def markdown2(args: Any*): Markdown2 = {
      Markdown2(sc.s(args: _*))
    }
    def md2(args: Any*) = markdown2(args: _*)
    def markdownR2(args: Any*): Markdown2 = {
      Markdown2(sc.raw(args: _*))
    }
    def mdR2(args: Any*) = markdownR2(args: _*)
  }
  implicit class HtmlHelper(val sc: StringContext) extends AnyVal {
    def html(args: Any*): Html = {
      Html(sc.s(args: _*))
    }
    def htmlR(args: Any*): Html = {
      Html(sc.raw(args: _*))
    }
  }
  implicit class HtmlHelper2(val sc: StringContext) extends AnyVal {
    def html2(args: Any*): Html2 = {
      Html2(sc.s(args: _*))
    }
    def htmlR2(args: Any*): Html2 = {
      Html2(sc.raw(args: _*))
    }
  }

  def desugar[T](code: T): Unit = ??? // via annotation macro

  def desugar2[T](code: T): Unit = macro ScalaKataMacro.desugar2_impl[T]

  def trace: Any ⇒ Unit = macro ScalaKataMacro.trace_implf
  def print: Any ⇒ Unit = macro ScalaKataMacro.trace_implf
  def println: Any ⇒ Unit = macro ScalaKataMacro.trace_implf
  def oprintln: Any ⇒ Unit = (a) ⇒ scala.Predef.println(a)
}
