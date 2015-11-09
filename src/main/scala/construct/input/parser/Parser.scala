package construct.input.parser

import scala.util.parsing.combinator._
import construct.input.ast._

object ConstructParser extends JavaTokenParsers with PackratParsers {

    // parsing interface
    def apply(s: String): ParseResult[Program] = parseAll(program, s)
    def parseStatement(s: String): ParseResult[Statement] = parseAll(statement, s)

    lazy val sep: PackratParser[String] =
      sys.props("line.separator") | ";" | "\n"

    lazy val seps: PackratParser[List[String]] = sep.*

    lazy val path: Parser[String] =
      """[\w\.]+""".r

    lazy val include: PackratParser[Path] =
      "include"~"<"~path~">" ^^ {case "include"~"<"~p~">" => Path(p)}

    lazy val includes: PackratParser[List[Path]] =
      repsep(include,rep1(sep))

    lazy val program: PackratParser[Program] =
      ( includes~seps~repsep(construction,sep.*)<~sep.* ^^
      {case includes~s1~constructions => Program(includes, constructions)})

    lazy val construction: PackratParser[Construction] =
      ( name~sep~params~sep~repsep(statement,sep)~sep~returns ^^
      {case name~s1~params~s2~states~s3~returns =>
        Construction(name, params, states, returns) })

    lazy val name: PackratParser[Identifier] =
      ( "construction"~id ^^ {case "construction"~id => id} )

    lazy val returns: PackratParser[List[Identifier]] =
      (   "return"~"points"~repsep(id,",") ^^ {case "return"~"points"~ids => ids   }
        | "return"                         ^^ {case "return"              => List()})

    lazy val params: PackratParser[List[Identifier]] =
      ("given"~"points"~ids ^^ {case "given"~"points"~ids => ids})

    lazy val statement: PackratParser[Statement] =
      "let"~ids~"="~id~"("~ids~")" ^^ {case "let"~outs~"="~fn~"("~ins~")" => Statement(outs, fn, ins)}

    lazy val ids: PackratParser[List[Identifier]] = rep1sep(id,",")

    lazy val id: Parser[Identifier] = """[\w_]+""".r ^^ {case s => Identifier(s)}
}
