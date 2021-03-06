/*
 * Copyright 2019 Erlend Hamnaberg
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

package bandaid

import org.specs2.mutable._
import org.json4s.JsonAST._
import org.json4s.native.JsonMethods._

class PointerSpec extends Specification {
  val rfcJson = parse(io.Source.fromInputStream(getClass.getResourceAsStream("/test.json")).mkString)
  val pointer = Pointer(rfcJson)

  "A RFC pointer" should {
    "select the whole document using empty string" in {
      pointer.select("") must beEqualTo(Some(rfcJson))
    }
    "select \"foo\" array property using /foo" in {
      pointer.select("/foo") must beEqualTo(Some(JArray(List(JString("bar"), JString("baz")))))
    }
    "select first item in \"foo\" array property using /foo/0" in {
      pointer.select("/foo/0") must beEqualTo(Some(JString("bar")))
    }
    "select \"\"    property using /" in {
      pointer.select("/") must beEqualTo(Some(JInt(0)))
    }
    "select \"a/b\" property using /a~1b" in {
      pointer.select("/a~1b") must beEqualTo(Some(JInt(1)))
    }    
    "select \"c%d\" property using /c%d" in {
      pointer.select("/c%d") must beEqualTo(Some(JInt(2)))
    }
    "select \"e^f\" property using /e^f" in {
      pointer.select("/e^f") must beEqualTo(Some(JInt(3)))
    }
    "select \"g|h\" property using /g|h" in {
      pointer.select("/g|h") must beEqualTo(Some(JInt(4)))
    }
    "select \"i\\j\" property using /i\\j" in {
      pointer.select("/i\\j") must beEqualTo(Some(JInt(5)))
    }
    "select \"k\"l\" property using /k\"l" in {
      pointer.select("/k\"l") must beEqualTo(Some(JInt(6)))
    }
    "select \" \"   property using / " in {
      pointer.select("/ ") must beEqualTo(Some(JInt(7)))
    }
    "select \"m~n\" property using /m~0n" in {
      pointer.select("m~n") must beEqualTo(Some(JInt(8)))
    }

    "replace \"foo\" array property using /foo" in {
      val expected = JArray(List(JString("a"), JString("whole"), JString("new"), JString("thing")))
      val update = pointer.update("/foo", expected)
      Pointer(update).select("/foo") must beEqualTo(Some(expected))
    }
    "add \"xxx\" array property using /xxx" in {
      val expected = JString("Hello")
      val update = pointer.add("/xxx", expected)
      Pointer(update).select("/xxx") must beEqualTo(Some(expected))
    }

    "replace first item in \"foo\" array property using /foo/0" in {
      val expected = JString("new")
      val update = pointer.update("/foo/0", expected)
      Pointer(update).select("/foo/0") must beEqualTo(Some(expected))
    }

    "add to \"foo\" array property using /foo/2" in {
      val expected = JString("new")
      val update = pointer.add("/foo/2", expected)
      Pointer(update).select("/foo/1") must beEqualTo(Some(JString("baz")))
      Pointer(update).select("/foo/2") must beEqualTo(Some(expected))
    }
    "remove \"foo\" array property using /foo" in {
      val update = pointer.remove("/foo")
      Pointer(update).select("/foo") must beNone
    }
    "remove 2nd item in \"foo\" array property using /foo/1" in {
      val update = pointer.remove("/foo/1")
      Pointer(update).select("/foo/1") must beNone
    }
  }
}