package com.scalakata.eval

// based on twitter util eval
// https://github.com/twitter/util/tree/master/util-eval

import java.io._
import java.math.BigInteger
import java.net.URLClassLoader
import java.security.MessageDigest
import java.util.Random

import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.interpreter.AbstractFileClassLoader
import scala.tools.nsc.io.{AbstractFile, VirtualDirectory}
import scala.tools.nsc.util.{BatchSourceFile, Position}
import scala.tools.nsc.reporters.StoreReporter

class Eval(settings: Settings) {

  private val jvmId = java.lang.Math.abs(new Random().nextInt())

  private val reporter = new StoreReporter()
  
  
  private val target = new VirtualDirectory("(memory)", None)
  private var classLoader = new AbstractFileClassLoader(target, this.getClass.getClassLoader)
  
  settings.outputDirs.setSingleOutput(target)

  private val compiler = new Global(settings, reporter)

  def apply(code: String): (Option[Any], Map[String, List[(Int, String)]]) = {
    val id = uniqueId(code)
    val className = "Evaluator__" + id
    compile(wrapCodeInClass(className, code), className)
    
    val infos = check(className)
    val infoss = infos.map{case (k, v) => (k.toString, v)}

    if(!infos.contains(reporter.ERROR)) {
      val cls = classLoader.loadClass(className)
      val t: Any = cls.getConstructor().newInstance().asInstanceOf[() => Any].apply().asInstanceOf[Any]
      (Some(t), infoss)
    } else {
      (None, infoss)
    }
  }

  private def uniqueId(code: String, idOpt: Option[Int] = Some(jvmId)): String = {
    val digest = MessageDigest.getInstance("SHA-1").digest(code.getBytes())
    val sha = new BigInteger(1, digest).toString(16)
    idOpt match {
      case Some(id) => sha + "_" + jvmId
      case _ => sha
    }
  }

  private def check(className: String): Map[reporter.Severity, List[(Int, String)]] = {
    reporter.infos.map {
      info => (
        info.severity,
        info.pos.point - preWrap(className).length,
        info.msg
      )
    }.to[List]
     .filterNot{ case (sev, _, msg) =>
      // annoying
      sev == reporter.WARNING &&
      msg == ("a pure expression does nothing in statement " +
              "position; you may be omitting necessary parentheses")
    }.groupBy(_._1)
     .mapValues{_.map{case (a,b,c) => (b,c)}}
  }

  private def preWrap(className: String) =
    "class " + className + " extends (() => Any) {\n" +
    "  def apply() = {\n"

  private def wrapCodeInClass(className: String, code: String) = {
    preWrap(className) +
    code + "\n" +
    "  }\n" +
    "}\n"
  }

  private def reset() {
    target.clear
    reporter.reset
    classLoader = new AbstractFileClassLoader(target, this.getClass.getClassLoader)
  }

  private def compile(code: String, className: String): Unit = {
    reset()
    val run = new compiler.Run
    val sourceFiles = List(new BatchSourceFile("(inline)", code))

    run.compileSources(sourceFiles)
  }
}