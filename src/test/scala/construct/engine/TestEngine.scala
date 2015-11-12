package construct.engine

//import scala.language.implicitConversions
import org.scalatest.Matchers
import org.scalatest.FunSuite

class EngineTest extends FunSuite with Matchers {

  test("Parallel Lines") {
    val l1 = Line(Point(0,0),Point(1,1))
    val l2 = Line(Point(1,0),Point(2,1))
    val intersection = Union(Set())
    l1 intersect l2 should be (intersection)
    l2 intersect l1 should be (intersection)
  }

  test("Identical Lines") {
    val l1 = Line(Point(0,0),Point(1,1))
    val l2 = Line(Point(-1,-1),Point(2,2))
    l1 intersect l2 should be (l1)
    l2 intersect l1 should be (l2)
  }

  test("Lines that intersect at defintion point") {
    val l1 = Line(Point(0,0),Point(1,0))
    val l2 = Line(Point(1,0),Point(2,1))
    val intersection = Point(1,0)
    l1 intersect l2 should be (intersection)
    l2 intersect l1 should be (intersection)
  }

  test("Sloped Lines that intersect 1") {
    val l1 = Line(Point(0,0),Point(3,4))
    val l2 = Line(Point(4,0),Point(5,4))
    val intersection = Point(6,8)
    l1 intersect l2 should be (intersection)
    l2 intersect l1 should be (intersection)
  }

  test("Sloped Lines that intersect 2") {
    val l1 = Line(Point(0,0),Point(4,5))
    val l2 = Line(Point(0,6),Point(2,7))
    val intersection = Point(8,10)
    l1 intersect l2 should be (intersection)
    l2 intersect l1 should be (intersection)
  }

  test("x and y lines") {
    val l1 = Line(Point(0,0),Point(4,0))
    val l2 = Line(Point(5,6),Point(5,7))
    val intersection = Point(5,0)
    l1 intersect l2 should be (intersection)
    l2 intersect l1 should be (intersection)
  }

  test("Two horizontal lines") {
    val l1 = Line(Point(0,0),Point(4,0))
    val l2 = Line(Point(3,6),Point(5,6))
    val intersection = Union(Set())
    l1 intersect l2 should be (intersection)
    l2 intersect l1 should be (intersection)
  }

  test("Two vertical lines") {
    val l1 = Line(Point(0,0),Point(0,4))
    val l2 = Line(Point(2,1),Point(2,6))
    val intersection = Union(Set())
    l1 intersect l2 should be (intersection)
    l2 intersect l1 should be (intersection)
  }

  test("Line circle no intersection") {
    val l = Line(Point(0,0), Point(1,1))
    val c = Circle(Point(-1, 2), Point(0, 1))
    val intersection = Union(Set())
    l intersect c should be (intersection)
    c intersect l should be (intersection)
  }

  test("Line circle one intersection definition point") {
    val l = Line(Point(0,0), Point(1,1))
    val c = Circle(Point(0, 2), Point(1, 1))
    val intersection = Point(1, 1)
    l intersect c should be (intersection)
    c intersect l should be (intersection)
  }

  test("Line circle two intersection") {
    val l = Line(Point(-1,-1), Point(2,2))
    val c = Circle(Point(-3, 4), Point(-7, 7))
    val intersection = Union(Set(Point(0, 0), Point(1, 1)))
    l intersect c should be (intersection)
    c intersect l should be (intersection)
  }

  test("Vertical line circle one intersection definition point") {
    val l = Line(Point(1,0), Point(1,1))
    val c = Circle(Point(0, 2), Point(-1, 2))
    val intersection = Point(1, 2)
    l intersect c should be (intersection)
    c intersect l should be (intersection)
  }

  test("Vertical line circle two intersection") {
    val l = Line(Point(0,2), Point(0,0))
    val c = Circle(Point(-4, 1), Point(-8, 4))
    val intersection = Union(Set(Point(0, 4), Point(0, -2)))
    l intersect c should be (intersection)
    c intersect l should be (intersection)
  }

  test("Interior circles no intersection") {
    val c1 = Circle(Point(0,0), Point(10,0))
    val c2 = Circle(Point(3,0), Point(9,0))
    val intersection = Union(Set())
    c1 intersect c2 should be (intersection)
    c2 intersect c1 should be (intersection)
  }

  test("Interior circles one intersection") {
    val c1 = Circle(Point(0,0), Point(10,0))
    val c2 = Circle(Point(3,0), Point(-4,0))
    val intersection = Point(10,0)
    c1 intersect c2 should be (intersection)
    c2 intersect c1 should be (intersection)
  }

  test("Interior circles two intersections") {
    val c1 = Circle(Point(0,0), Point(5,0))
    val c2 = Circle(Point(4,0), Point(1,0))
    val intersection = Union(Set(Point(4,3),Point(4,-3)))
    c1 intersect c2 should be (intersection)
    c2 intersect c1 should be (intersection)
  }

  test("Exterior circles no intersection") {
    val c1 = Circle(Point(0,0), Point(10,0))
    val c2 = Circle(Point(20,0), Point(29,0))
    val intersection = Union(Set())
    c1 intersect c2 should be (intersection)
    c2 intersect c1 should be (intersection)
  }

  test("Exterior circles one intersection") {
    val c1 = Circle(Point(0,0), Point(0,-10))
    val c2 = Circle(Point(13,0), Point(16,0))
    val intersection = Point(10,0)
    c1 intersect c2 should be (intersection)
    c2 intersect c1 should be (intersection)
  }

  test("Exterior circles two intersections") {
    val c1 = Circle(Point(0,0), Point(5,0))
    val c2 = Circle(Point(8,0), Point(3,0))
    val intersection = Union(Set(Point(4,3),Point(4,-3)))
    c1 intersect c2 should be (intersection)
    c2 intersect c1 should be (intersection)
  }

  test("Identicle circles") {
    val c1 = Circle(Point(0,0), Point(5,0))
    val c2 = Circle(Point(0,0), Point(-5,0))
    c1 intersect c2 should be (c1)
    c2 intersect c1 should be (c2)
  }

  test("Union of lines and line, 2 intersections") {
    val l11 = Line(Point(0,0), Point(0,10))
    val l12 = Line(Point(10,10), Point(5,10))
    val u1 = Union(Set(l12, l11))
    val l2 = Line(Point(0,5), Point(10,15))
    val intersection = Union(Set(Point(0,5), Point(5,10)))
    u1 intersect l2 should be (intersection)
    l2 intersect u1 should be (intersection)
  }

  test("Union of lines and line, 1 intersection") {
    val l11 = Line(Point(0,0), Point(0,10))
    val l12 = Line(Point(10,10), Point(15,15))
    val u1 = Union(Set(l12, l11))
    val l2 = Line(Point(0,5), Point(10,15))
    val intersection = Union(Set(Point(0,5)))
    u1 intersect l2 should be (intersection)
    l2 intersect u1 should be (intersection)
  }

  test("Union of lines and union of line, 2 intersections") {
    val l11 = Line(Point(0,0), Point(0,10))
    val l12 = Line(Point(10,10), Point(5,10))
    val u1 = Union(Set(l12, l11))
    val l2 = Line(Point(0,5), Point(10,15))
    val u2 = Union(Set(l2))
    val intersection = Union(Set(Point(0,5), Point(5,10)))
    u1 intersect u2 should be (intersection)
    u2 intersect u1 should be (intersection)
  }

  test("Union of lines and union of line, 1 intersection") {
    val l11 = Line(Point(0,0), Point(0,10))
    val l12 = Line(Point(10,10), Point(15,15))
    val u1 = Union(Set(l12, l11))
    val l2 = Line(Point(0,5), Point(10,15))
    val u2 = Union(Set(l2))
    val intersection = Union(Set(Point(0,5)))
    u1 intersect u2 should be (intersection)
    u2 intersect u1 should be (intersection)
  }
}

