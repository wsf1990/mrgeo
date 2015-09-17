package org.mrgeo.mapalgebra


import java.io.File
import java.lang.reflect.Modifier

import grizzled.net.url
import org.apache.spark.Logging
import org.clapper.classutil.ClassInfo
import org.mrgeo.core.MrGeoProperties
import org.mrgeo.mapalgebra.parser.ParserNode
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.{ClasspathHelper, ConfigurationBuilder}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.reflect.runtime.universe._


object MapOpFactory extends Logging {
  val functions = mutable.HashMap.empty[String, MapOpRegistrar]

  private def registerFunctions() = {
    val start = System.currentTimeMillis()

    val mapops = decendants(classOf[MapOpRegistrar]) getOrElse Set.empty

    logInfo("Registering MapOps:")

    val mirror = runtimeMirror(Thread.currentThread().getContextClassLoader) // obtain runtime mirror

    mapops.foreach(symbol => {
      mirror.reflectModule(symbol.asModule).instance match {
      case mapop: MapOpRegistrar =>
        logInfo("  " + mapop)
        mapop.register.foreach(op => {
          functions.put(op, mapop)
        })
      case _ =>
      }
    })

    val time = System.currentTimeMillis() - start

    logInfo("Registration took " + time + "ms")
  }

  // create a mapop from a function name, called by MapOpFactory("<name>")
  def apply(node: ParserNode, variables: String => Option[ParserNode], protectionLevel:String = null): Option[MapOp] = {
    if (functions.isEmpty) {
      registerFunctions()
    }

    functions.get(node.getName) match {
    case Some(mapop) =>
      val op = mapop.apply(node, variables, protectionLevel)
      Some(op)
    case None => None
    }
  }

  def exists(name: String): Boolean = {
    if (functions.isEmpty) {
      registerFunctions()
    }

    functions.contains(name)
  }

  private def decendants(clazz: Class[_]) = {

    // get all the URLs for this classpath, filter files by "mrgeo" in development mode, then strip .so files
    val urls = (ClasspathHelper.forClassLoader() ++ ClasspathHelper.forJavaClassPath()).filter(url => {
      val file = new File(url.getPath)

      file.isDirectory || (if (MrGeoProperties.isDevelopmentMode) file.getName.contains("mrgeo") else true)
    }
    ).filter(!_.getFile.endsWith(".so"))


    // register mapops
    val cfg = new ConfigurationBuilder()
        .setUrls(urls)
        .setScanners(new SubTypesScanner())
        .useParallelExecutor()

    val reflections: Reflections = new Reflections(cfg)
    //val reflections: Reflections = new Reflections("org.mrgeo")

    val classes = reflections.getSubTypesOf(clazz).filter(p => !Modifier.isAbstract(p.getModifiers))

    val mirror = runtimeMirror(Thread.currentThread().getContextClassLoader) // obtain runtime mirror

    Some(classes.map(clazz => {
      val sym = mirror.classSymbol(clazz)

      if (sym.companionSymbol != null) {
        sym.companionSymbol
      }
      else {
        sym
      }
    }))
  }
}

