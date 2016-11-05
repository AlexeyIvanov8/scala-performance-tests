package com.skn.mapper

import com.skn.model.ViewItem

import scala.reflect.runtime.{universe => ru}

/**
  * Simple converter from ViewItem to String via some rules, only for emulate some work
  */
class ViewMapper {

  // cases definitions
  case class ReflectData(mirror: ru.Mirror, classType: ru.Type, vars: Map[String, ru.FieldMirror])

  var reflectCache = Map[Class[_], ReflectData]()

  /**
    * Try find reflection data of item in local cache. If not found then fetch reflect data
    * and put in cache.
    * @param item
    * @return - data of type passed item
    */
  private def cacheReflectData(item: Any): ReflectData = {
    val itemClass = item.getClass
    if (!reflectCache.contains(itemClass)) {
      val mirror = ru.runtimeMirror(itemClass.getClassLoader)

      val classSymbol = mirror.classSymbol(itemClass)

      val reflectItem = mirror.reflect(item)

      val reflectItemClass = classSymbol.asClass
      val vars = reflectItemClass.info.members
        .filter(_.isTerm)
        .map(_.asTerm)
        .filter(m => m.isVal || m.isVar)
        .map(field => field.getter.name.toString -> reflectItem.reflectField(field))
        .toMap

      reflectCache += (itemClass -> ReflectData(mirror, reflectItem.symbol.toType, vars))
    }
    reflectCache(itemClass)
  }

  val ViewItemType = ru.typeOf[ViewItem]
  val SeqType = ru.typeOf[Seq[_]]

  /**
    * Entry point for conversion. Take ViewItem and return they String representation
   */
  def startWrite(item: ViewItem): String = {
    "{\n" + write(item, 0) + "\n}"
  }

  /**
    * Main conversion method
    * @param item - for conversion
    * @param padding - for build pretty JSON like view
    * @return string representation of item
    */
  private def write(item: Any, padding: Int): String = {
    val reflectData = cacheReflectData(item)
    reflectData.vars.map { case (name, desc) =>
      val value = desc.bind(item).get
      (value match {
        case v: ViewItem => write(value, padding + 1)
        case v: Seq[_] =>  value.asInstanceOf[Seq[_]]
          .map { elt => write(elt, padding + 1) }
          .reduceLeftOption((l, r) => l + r)
          .getOrElse("")
        case _ => value.toString
      })

        // for convert to JSON like representation. Need more additional CPU work
//      name + ": " + (/*variableReflectData.classType*/value match {
//        /*case d if d <:< ViewItemType*/case v: ViewItem => "{\n" + write(value, padding + 1) + "}"
//        /*case d if d <:< SeqType*/case v: Seq[_] => "[\n" + value.asInstanceOf[Seq[_]]
//          .map { elt => write(elt, padding + 1) }
//          .reduceLeftOption((l, r) => l + ",\n" + r)
//          .getOrElse("") + "]"
//        case _ => value.toString
//      })
      //for(i <- 0 to padding) out = " " + out

    }
      .reduceLeftOption((l, r) => l + "\n" + r)
      .getOrElse("")
  }
}
