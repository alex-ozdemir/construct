// Alex Ozdemir <aozdemir@hmc.edu>
// Dec 2015
//
// This files holds the parser for construct
// It has multiple entry points, most are added to support the GREPL

package construct.input.parser

import scala.util.parsing.combinator._
import construct.input.ast._

object ConstructParser extends JavaTokenParsers with PackratParsers {

  // Newlines are significant, and thus omitted from this definition
  override protected val whiteSpace = """[ \t]+""".r

  // whole program parsing interface
  def apply(s: String): ParseResult[Program] = parseAll(program, s)

  // parsing interfaces for sub-parts of a program, used by GREPL
  def parseStatement(s: String): ParseResult[Statement] = parseAll(statement, s)
  def parseInclude(s: String): ParseResult[Path] = parseAll(include, s)
  def parseGivens(s: String): ParseResult[List[Parameter]] = parseAll(givens, s)
  def parseReturns(s: String): ParseResult[List[Identifier]] = parseAll(returns, s)
  def parseSuggestionTake(s: String): ParseResult[(Pattern, String)] =
    parseAll(suggestionTake, s)

  lazy val sug_id: Parser[String] = """\d+""".r

  lazy val suggestionTake: PackratParser[(Pattern, String)] =
    (   pattern_plus~sug_id ^^ {case pattern~id => (pattern, id)}
      | failure("Could not parse the suggestion usage") )

  lazy val sep: PackratParser[String] =
    sys.props("line.separator") | ";"~>sys.props("line.separator") | ";"

  lazy val seps: PackratParser[List[String]] = sep.+

  lazy val csep: Parser[String] = """,""".r

  lazy val param: PackratParser[Parameter] =
    (   id~id ^^ {case ty~name => Parameter(name, ty)}
      | failure("Problem with parameter") )

  lazy val path: Parser[String] = """[\w/\.]+""".r

  lazy val include: PackratParser[Path] =
    (   "include"~>path ^^ {case p => Path(p)}
      | failure("Expected include, like 'include file.con'") )

  lazy val includes: PackratParser[List[Path]] =
    (   rep1sep(include,seps)<~seps
      | "" ^^ {case "" => List()}
      | failure("Error parsing includes") )

  lazy val program: PackratParser[Program] =
    (   (seps.*)~>includes~items<~sep.* ^^ {case ins~items => Program(ins,items)}
      | failure("Problem with program structure") )

  lazy val items: PackratParser[List[Item]] =
    (   repsep(item, seps)
      | failure("Problem parsing items") )

  lazy val item: PackratParser[Item] =
    ( shape | construction | failure("Expected an item or construction") )

  lazy val shape: PackratParser[Shape] =
    (   "shape"~>id~sep~givens~sep~repsep(statement,sep)~sep~returns ^^
          {case name~s1~givens~s2~states~s3~returns =>
            Shape(Construction(name, givens, states, returns)) }
      | failure("Problem with shape declaration structure") )

  lazy val construction: PackratParser[Construction] =
    (   name~sep~givens~sep~repsep(statement,sep)~sep~returns ^^
          {case name~s1~givens~s2~states~s3~returns =>
            Construction(name, givens, states, returns) }
      | failure("Problem with construction structure") )

  lazy val name: PackratParser[Identifier] =
    ( "construction"~>id
      | failure("Problem with construction name") )

  lazy val returns: PackratParser[List[Identifier]] =
    (   "return"~>repsep(id,csep) ^^ {case ids      => ids}
      | "return"                  ^^ {case "return" => List()}
      | failure("Problem parsing the contruction's returns") )

  lazy val givens: PackratParser[List[Parameter]] =
    (   "given" ~ "points" ~> ids ^^ { _ map { Parameter(_, Identifier("point")) } }
      | "given" ~> rep1sep(param,csep)
      | failure("Problem parsing the given points for the construction") )

  lazy val statement: PackratParser[Statement] =
    (   pattern_plus~expr ^^ {case pattern~expr => Statement(pattern, expr)}
      | failure("Problem parsing statement") )

  lazy val expr: PackratParser[Expr] =
    (   id~"("~rep1sep(expr,csep)<~")" ^^ {case id~"("~exprs => FnApp(id, exprs)}
      | id                             ^^ {case id => Exactly(id)}
      | failure("Problem parsing expression") )

  lazy val pattern_plus: PackratParser[Pattern] =
    (   "let"~>pattern_in <~"="
      | "let"~>pattern_ins<~"=" ^^ {case patterns => Tuple(patterns)}
      | failure("Could not parse pattern") )

  lazy val pattern_in: PackratParser[Pattern] =
    (   "("~>pattern_ins<~")"    ^^ {case patterns        => Tuple(patterns)}
      | id~"("~pattern_ins<~")"  ^^ {case id~"("~patterns => Destructor(id, patterns)}
      | id                       ^^ {case id              => Id(id)}
      | failure("Problem parsing pattern") )

  lazy val pattern_ins: PackratParser[List[Pattern]] =
    ( rep1sep(pattern_in,csep) | failure("Problem parsing comma delimited patterns") )

  lazy val ids: PackratParser[List[Identifier]] =
    ( rep1sep(id,csep) | failure("Problem with identifier list") )

  lazy val id: Parser[Identifier] = 
    (   """[\w_]+""".r ^^ {case s => Identifier(s)}
      | failure("Problem parsing identifier") )
}
