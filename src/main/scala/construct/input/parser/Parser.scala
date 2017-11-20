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
  def apply(s: String): ParseResult[Program] = parseAll(commit(program), s)

  // parsing interfaces for sub-parts of a program, used by GREPL
  def parseStatement(s: String): ParseResult[Statement] = parseAll(commit(statement), s)
  def parseInclude(s: String): ParseResult[Path] = parseAll(commit(include), s)
  def parseGivens(s: String): ParseResult[List[Parameter]] = parseAll(commit(givens), s)
  def parseReturns(s: String): ParseResult[List[Identifier]] = parseAll(commit(returns), s)
  def parseSuggestionTake(s: String): ParseResult[(Pattern, String)] =
    parseAll(commit(suggestionTake), s)

  lazy val sug_id: Parser[String] = """\d+""".r

  lazy val suggestionTake: PackratParser[(Pattern, String)] =
    pattern_plus~sug_id ^^ {case pattern~id => (pattern, id)} withFailureMessage
      "Could not parse this suggestion usage"

  lazy val sep: PackratParser[String] =
    (sys.props("line.separator") | ";"~>sys.props("line.separator") | ";") withFailureMessage "Expected a line break"

  lazy val seps: PackratParser[List[String]] = sep.+ withFailureMessage "Expected a line break"

  lazy val csep: Parser[String] = """,""".r withFailureMessage "Expected a comma"

  lazy val param: PackratParser[Parameter] =
    id~id ^^ {case ty~name => Parameter(name, ty)} withFailureMessage "Expected a given preceeded by its type"

  lazy val path: Parser[String] = """[\w/\.]+""".r

  lazy val include: PackratParser[Path] =
    "include"~>path ^^ {case p => Path(p)} withFailureMessage "Expected an include statement"

  lazy val includes: PackratParser[List[Path]] =
    ((rep1sep(include,seps)<~seps).? ^^ { _.getOrElse(List()) }) withFailureMessage
      "Files can only begin with include statements"

  lazy val program: PackratParser[Program] =
    (seps.*)~>includes~items<~sep.* ^^ {case ins~items => Program(ins,items)}

  lazy val items: PackratParser[List[Item]] =
    repsep(item, seps)

  lazy val item: PackratParser[Item] =
    shape | construction

  lazy val shape: PackratParser[Shape] =
    "shape"~>id~sep~givens~sep~repsep(statement,sep)~sep~returns ^^
          {case name~s1~givens~s2~states~s3~returns =>
            Shape(Construction(name, givens, states, returns)) } withFailureMessage
            "Malformed shape"

  lazy val construction: PackratParser[Construction] =
    name~sep~givens~sep~statements~sep~returns ^^
          {case name~s1~givens~s2~states~s3~returns =>
            Construction(name, givens, states, returns) } //withFailureMessage "Malformed construction"

  lazy val name: PackratParser[Identifier] =
    "construction"~>id withFailureMessage "Malformed construction declaration"

  lazy val returns: PackratParser[List[Identifier]] =
    (   "return"~>repsep(id,csep) ^^ {case ids      => ids}
      | "return"                  ^^ {case "return" => List()}
    ) withFailureMessage "Malformed return statement"

  lazy val givens: PackratParser[List[Parameter]] =
    (   "given" ~ "points" ~> ids ^^ { _ map { Parameter(_, Identifier("point")) } }
      | commit("given" ~> rep1sep(param,csep))
    ) withFailureMessage "Expected givens for the construction"

  lazy val statements: PackratParser[List[Statement]] =
    repsep(statement,sep) withFailureMessage "Malformed sequence of statements"

  lazy val statement: PackratParser[Statement] =
    pattern_plus~expr ^^ {case pattern~expr => Statement(pattern, expr)}

  lazy val expr: PackratParser[Expr] =
    (   expr~"-"~expr                  ^^ {case e1~"-"~e2 => Difference(e1, e2)}
      | id~"("~rep1sep(expr,csep)<~")" ^^ {case id~"("~exprs => FnApp(id, exprs)}
      | "{"~>repsep(expr,csep)<~"}"    ^^ {SetLit(_)}
      | id                             ^^ {case id => Exactly(id)}
    ) withFailureMessage "Malformed expression"

  lazy val pattern_plus: PackratParser[Pattern] =
    (   "let"~>pattern_in <~"="
      | "let"~>pattern_ins<~"=" ^^ {case patterns => Tuple(patterns)}
    ) withFailureMessage "Each statement should begin with a pattern or variable binding"

  lazy val pattern_in: PackratParser[Pattern] =
    (   "("~>pattern_ins<~")"    ^^ {case patterns        => Tuple(patterns)}
      | id~"("~pattern_ins<~")"  ^^ {case id~"("~patterns => Destructor(id, patterns)}
      | id                       ^^ {case id              => Id(id)}
    ) withFailureMessage "Found a malformed pattern"

  lazy val pattern_ins: PackratParser[List[Pattern]] =
    rep1sep(pattern_in,csep)

  lazy val ids: PackratParser[List[Identifier]] =
    rep1sep(id,csep) withFailureMessage "Expected a comma-separated list of 1 or more identifiers"

  lazy val id: Parser[Identifier] = 
    (   """[\w_]+""".r ^^ {case s => Identifier(s)} ) withFailureMessage "Expected an identifier"
}
