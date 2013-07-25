/*
 * Copyright 2013 Erlend Hamnaberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pointy

import org.json4s.JsonAST._
import annotation.tailrec

/**
 Implementation of JSON Pointer. RFC-6901
 https://tools.ietf.org/html/rfc6901
*/
case class Pointer(document: JValue) {
  import Ref._

  def select[A](selector: A)(implicit s: Selector[A]): Option[JValue] = {
    @tailrec def recur(parts: List[Ref], in: JValue): Option[JValue] = {
      parts match {
        case Nil => in match {
          case JNothing => None
          case v => Some(v)
        }
        case ArrayRef(x) :: xs => in match {
          case JArray(list) => recur(xs, if (list.isDefinedAt(x)) list(x) else JNothing)
          case _ => None
        }
        case EndOfArray :: _ => sys.error("List index is out-of-bounds")
        case PropertyRef(name) :: xs => in match {
          case JObject(list) => recur(xs, list.find{case JField(n, v) => n == name}.map(_._2).getOrElse(JNothing))
          case _ => None
        }
      }
    }
    recur(s(selector), document)
  }

  def update[A](selector: A, replacement: JValue)(implicit s: Selector[A]): JValue = {
    def recur(s: List[Ref], in: JValue): JValue = {
      s match {
        case Nil => in
        case PropertyRef(name) :: xs => in match {
          case JObject(fields) => JObject(
             fields.map {
               case JField(`name`, v) => JField(name, if (xs == Nil) replacement else recur(xs , v))
               case field => field
             }.filterNot(_._1 == JNothing)
          )
          case other => other
        }
        case ArrayRef(i) :: xs => in match {
          case a@JArray(arr) => if (xs == Nil) {
            if (arr.isDefinedAt(i)) JArray(arr.updated(i, replacement).filterNot(_ == JNothing)) else sys.error("List index '%s' is out-of-bounds".format(i))
          } else recur(xs, a)
          case other => other
        }
        case EndOfArray :: xs => sys.error("List index is out-of-bounds")
      }
    }
    recur(s(selector), document)
  }

  def add[A](selector: A, toAdd: JValue)(implicit s: Selector[A]): JValue = {
    def recur(s: List[Ref], in: JValue): JValue = {
      s match {
        case Nil => in
        case PropertyRef(name) :: xs => in match {
          case JObject(fields) => JObject(
            fields.map {
              case JField(`name`, v) => JField(name, if (xs == Nil) sys.error("Property with name %s already exists".format(name)) else recur(xs , v))
              case field => field
            } :+ JField(name, toAdd)
          )
          case other => other
        }
        case ArrayRef(i) :: xs => in match {
          case a@JArray(arr) => if (xs == Nil) JArray(arr.patch(i + 1, List(toAdd), 0)) else recur(xs, a)
          case other => other
        }
        case EndOfArray :: xs => in match {
          case a@JArray(arr) => if (xs == Nil) JArray(arr :+ toAdd) else recur(xs, a)
          case other => other
        }
      }
    }
    recur(s(selector), document)
  }

  def remove[A](selector: A)(implicit s: Selector[A]): JValue = update(selector, JNothing)

}
