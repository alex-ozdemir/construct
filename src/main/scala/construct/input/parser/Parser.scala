package construct.input.parser

import scala.util.parsing.combinator._
import construct.input.ast._

object ConstructParser extends JavaTokenParsers with PackratParsers {

  override val skipWhitespace = false

  // parsing interface
  //def apply(s: String): ParseResult[Construction] = parseAll(construction, s)
  def apply(s: String): ParseResult[Program] = parseAll(program, s)
  def parseStatement(s: String): ParseResult[Statement] = parseAll(statement, s)
  def parseInclude(s: String): ParseResult[Path] = parseAll(include, s)
  def parseGivens(s: String): ParseResult[List[Identifier]] = parseAll(givens, s)

  lazy val sep: PackratParser[String] =
    sys.props("line.separator") | ";"~>sys.props("line.separator") | ";"

  lazy val seps: PackratParser[List[String]] = sep.+

  lazy val csep: Parser[String] = """, *""".r

  lazy val sp: Parser[String] = """[ \t]+""".r

  lazy val path: Parser[String] =
    """[\w\.]+""".r

  lazy val include: PackratParser[Path] =
    (   "include"~sp~>path ^^ {case p => Path(p)}
      | failure("Expected include, like 'include file.con'") )

  lazy val includes: PackratParser[List[Path]] =
    (   rep1sep(include,seps)<~seps
      | "" ^^ {case "" => List()}
      | failure("Error parsing includes") )

  lazy val program: PackratParser[Program] =
    (   includes~constructions<~sep.* ^^ {case ins~cons => Program(ins,cons)}
      | failure("Problem with program structure") )

  lazy val constructions: PackratParser[List[Construction]] =
    (   repsep(construction, seps)
      | failure("Problem parsing constructions") )

  lazy val construction: PackratParser[Construction] =
    ( name~sep~givens~sep~repsep(statement,sep)~sep~returns ^^
    {case name~s1~givens~s2~states~s3~returns =>
      Construction(name, givens, states, returns) } |
      failure("Problem with construction structure") )

  lazy val name: PackratParser[Identifier] =
    ( "construction"~sp~>id
      | failure("Problem with construction name") )

  lazy val returns: PackratParser[List[Identifier]] =
    (   "return"~sp~"points"~sp~>repsep(id,csep) ^^ {case ids      => ids}
      | "return"                                 ^^ {case "return" => List()}
      | failure("Problem parsing the contruction's returns") )

  lazy val givens: PackratParser[List[Identifier]] =
    (   "given"~sp~"points"~sp~>ids
      | failure("Problem parsing the given points for the construction") )

  lazy val statement: PackratParser[Statement] =
    (   "let"~sp~>ids~sp~"="~sp~id~"("~ids~")" ^^
          {case outs~s1~"="~s2~fn~"("~ins~")" => Statement(outs, fn, ins)}
      | failure("Problem parsing statement") )

  lazy val ids: PackratParser[List[Identifier]] = 
    ( rep1sep(id,csep) | failure("Problem with identifier list") )

  lazy val id: Parser[Identifier] = 
    (   """[\w_]+""".r ^^ {case s => Identifier(s)}
      | failure("Problem parsing identifier") )
}
