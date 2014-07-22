package com.scalakata.eval

import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Random
import java.util.concurrent.{TimeoutException, Callable, FutureTask, TimeUnit}

import scala.util.control.NonFatal
import scala.concurrent.duration._

import scala.tools.nsc.interactive.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.StoreReporter
import scala.tools.nsc.io.VirtualDirectory
import scala.reflect.internal.util._
import scala.tools.nsc.interactive.Response

class Compiler(artifacts: String, scalacOptions: Seq[String]) {

  def insight(code: String): EvalResponse = {
    if (code.isEmpty) EvalResponse.empty
    else {
      try { 
        withTimeout{
          eval(code) match {
            case (Some(instr), cinfos) ⇒

              val i = 
                instr.map{ case ((start, end), value) ⇒
                  val (v, xml) = value match {
                    case a: String ⇒ {
                      val quote = 
                        if (a.lines.size > 1) "\"\"\""
                        else "\""
                      (quote + a + quote, false)
                    }
                    case xml: scala.xml.Elem ⇒
                      (xml.toString, true)
                    case other ⇒ (other.toString, false)
                  }
                  Instrumentation(v, xml, start, end)
                }.to[List]

              EvalResponse.empty.copy(
                insight = i,
                infos = convert(cinfos)
              )
            case (_, cinfos) ⇒ EvalResponse.empty.copy(infos = convert(cinfos))
          }

        }(timeout).getOrElse(
          EvalResponse.empty.copy(timeout = true)
        )
      } catch {
        case NonFatal(e) ⇒ {

          def virtual(line: Int) = -line
          val pos = virtual(e.getCause.getStackTrace.drop(1).head.getLineNumber)

          EvalResponse.empty.copy(runtimeError = 
            Some(RuntimeError(e.getCause.toString, pos - eval.lineOffset)) 
          )
        }
      }
    }
  }

  private val beginWrap = "class ScalaKata {\n"
  private val endWrap = "\n}"

  private val wrapOffset = beginWrap.size

  private def wrap(code: String): BatchSourceFile = {
    new BatchSourceFile("default", beginWrap + code + endWrap)
  }

  private def reload(code: String): BatchSourceFile = {
    val file = wrap(code)
    withResponse[Unit](r ⇒ compiler.askReload(List(file), r)).get
    file
  }

  private def withResponse[A](op: Response[A] ⇒ Any): Response[A] = {
    val response = new Response[A]
    op(response)
    response
  }

  def autocomplete(code: String, p: Int): List[CompletionResponse] = {
    if(code.isEmpty) Nil
    else {
      val file = reload(code)
      val ajustedPos = p + wrapOffset
      val pos = new OffsetPosition(file, ajustedPos)

      // TODO: https://github.com/scala-ide/scala-ide/blob/4.0.0-m3-luna/org.scala-ide.sdt.core/src/org/scalaide/core/completion/ScalaCompletions.scala#L170

      val response1 = withResponse[List[compiler.Member]](r ⇒ 
        compiler.askTypeCompletion(pos, r)
      )

      val r1 = response1.get match {
        case Left(members) ⇒ compiler.ask( () ⇒ {
          members.map(member ⇒ 
            CompletionResponse(
              name = member.sym.decodedName,
              signature = member.sym.signatureString
            )
          )
        })
        case Right(e) ⇒ 
          e.printStackTrace
          Nil
      }

      val response2 = withResponse[List[compiler.Member]](r ⇒ 
        compiler.askScopeCompletion(pos, r)
      )

      val r2 = response2.get match {
        case Left(members) ⇒ compiler.ask( () ⇒ {
          members.map(member ⇒ 
            CompletionResponse(
              name = member.sym.decodedName,
              signature = member.sym.signatureString
            )
          )
        })
        case Right(e) ⇒ 
          e.printStackTrace
          Nil
      }

      (r1 ::: r2).distinct
    }
  }

  def typeAt(code: String, start: Int, end: Int): Option[TypeAtResponse] = {
    if(code.isEmpty) None
    else {
      val astart = start + wrapOffset
      val aend = end + wrapOffset

      val file = reload(code)
      val response = withResponse[compiler.Tree](r ⇒ 
        compiler.askTypeAt(compiler.rangePos(file, astart, astart, aend), r)
      )

      response.get match {
        case Left(tree) ⇒ compiler.ask( () ⇒ {
          // thanks ensime
          val res = 
            tree match {
              case compiler.Select(qual, name) =>
                qual
              case t: compiler.ImplDef if t.impl != null =>
                t.impl
              case t: compiler.ValOrDefDef if t.tpt != null =>
                t.tpt
              case t: compiler.ValOrDefDef if t.rhs != null =>
                t.rhs
              case t => t
            }
          Some(TypeAtResponse(res.tpe.toString))
        })
        case Right(e) ⇒ 
          e.printStackTrace
          None
      }
    }
  }

  private val timeout = 60.seconds
  private val jvmId = java.lang.Math.abs(new Random().nextInt())

  private val reporter = new StoreReporter()
  private val settings = new Settings()
  
  settings.processArguments(scalacOptions.to[List], true)
  settings.bootclasspath.value = artifacts
  settings.classpath.value = artifacts
  settings.Yrangepos.value = true

  private val compiler = new Global(settings, reporter)
  private val eval = new Eval(settings.copy)

  private def convert(infos: Map[String, List[(Int, String)]]): Map[Severity, List[CompilationInfo]] = {
    infos.map{ case (k,vs) ⇒ 
      val sev = k match {
        case "ERROR" ⇒ Error
        case "WARNING" ⇒ Warning
        case "INFO" ⇒ Info
      }
      (sev, vs map {case (p, m) ⇒ CompilationInfo(m, p)})
    }
  }

  private def withTimeout[T](f: ⇒ T)(timeout: Duration): Option[T]= {
    val task = new FutureTask(new Callable[T]() {
      def call = f
    })
    val thread = new Thread( task )
    try {
      thread.start()
      Some(task.get(timeout.toMillis, TimeUnit.MILLISECONDS))     
    } catch {
      case e: TimeoutException ⇒ None
    } finally { 
      if( thread.isAlive ){
        thread.interrupt()
        thread.stop()
      }
    }
  }
}