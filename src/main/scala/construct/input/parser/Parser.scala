package construct.input.parser

import scala.util.parsing.combinator._
import construct.input.ast._

object ConstructParser extends JavaTokenParsers with PackratParsers {

    // parsing interface
    def apply(s: String): ParseResult[Program] = parseAll(program, s)
    def parseStatement(s: String): ParseResult[Statement] = parseAll(statement, s)

    lazy val id: PackratParser[Identifier] =
      (ident ^^ {s => Identifier(s)})

    lazy val state_sep: PackratParser[String] =
      sys.props("line.separator") | ";"

    lazy val path: Parser[String] =
      """[\w\.]+""".r

    lazy val include: PackratParser[Path] =
      "include"~"<"~path~">" ^^ {case "include"~"<"~p~">" => Path(p)}

    lazy val includes: PackratParser[List[Path]] =
      repsep(include,rep1(state_sep))

    lazy val program: PackratParser[Program] =
      (includes~params~state_sep~rep1sep(statement,rep1(state_sep))~(state_sep.*) ^^
        {case includes~p~state_sep~list~tail =>
          Program(includes,
                  Some(Construction(Identifier(""), p, list, Returns(List()))))}
      | includes~params~state_sep~rep1sep(statement,rep1(state_sep))~state_sep~returns ^^
        {case includes~p~s1~list~s2~r =>
          Program(includes,
                  Some(Construction(Identifier(""), p, list, r)))})

    lazy val returns: PackratParser[Returns] =
      ("return"~"points"~repsep(id,",") ^^ {case "return"~"points"~ids => Returns(ids)})

    lazy val params: PackratParser[Parameters] =
      ("given"~"points"~rep1sep(id,",") ^^ {case "given"~"points"~ids => Parameters(ids)})

    lazy val statement: PackratParser[Statement] =
      (let | constructor | intersection)

    lazy val intersection: PackratParser[PointsIntersectionStatement] =
      ("let"~id~"and"~id~"intersect"~"at"~"points"~rep1sep(id,",")
        ^^ {case "let"~id1~"and"~id2~"intersect"~"at"~"points"~ids =>
            PointsIntersectionStatement(id1, id2, ids)})

    lazy val let: PackratParser[LetStatement] =
      ("let"~id~"be"~"a"~constructor
        ^^ {case "let"~name~"be"~"a"~cons => LetStatement(name, cons)})

    lazy val constructor: PackratParser[Constructor] =
      (circle | line | point)

    lazy val circle: PackratParser[CircleConstructor] =
      ("circle"~"with"~"center"~id~"and"~"edge"~id
        ^^ {case "circle"~"with"~"center"~c~"and"~"edge"~e => CircleConstructor(c, e)})

    lazy val line: PackratParser[LineConstructor] =
      ("line"~"from"~id~"to"~id
        ^^ {case "line"~"from"~a~"to"~b => LineConstructor(a, b)})

    lazy val point: PackratParser[PointConstructor] =
      ("point" ^^ {case "point" => PointConstructor()})
 }
