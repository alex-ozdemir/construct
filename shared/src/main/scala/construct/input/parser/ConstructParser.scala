// Alex Ozdemir <aozdemir@hmc.edu>
// Dec 2015
//
// This files holds the parser for construct
// It has multiple entry points, most are added to support the GREPL

package construct.input.parser

import scala.util.parsing.combinator._
import construct.input.ast._

import scala.util.matching.Regex

object ConstructParser extends JavaTokenParsers with PackratParsers {

  // Newlines are significant, and thus omitted from this definition
  override protected val whiteSpace: Regex = """[ \t]+""".r

  // whole program parsing interface
  def apply(s: String): ParseResult[Program] = parseAll(commit(program), s)

  def parseGREPLInstruction(s: String): ParseResult[GREPLInstruction] =
    parseAll(grepl_instruction, s)

  // parsing interfaces for sub-parts of a program, used by GREPL
  def parseSuggestionTake(s: String): ParseResult[(Pattern, String)] =
    parseAll(commit(suggestionTake), s)

  lazy val sug_id: Parser[String] = """\d+""".r

  lazy val suggestionTake: PackratParser[(Pattern, String)] =
    "let" ~> pattern ~ "=" ~ sug_id ^^ {
      case pat ~ "=" ~ i => (pat, i)
    } withFailureMessage
      "Could not parse this suggestion usage"

  lazy val sep: PackratParser[String] =
    (sys.props("line.separator") | ";" ~> sys.props("line.separator") | ";") withFailureMessage "Expected a line break"

  lazy val seps: PackratParser[List[String]] =
    sep.+ withFailureMessage "Expected a line break"

  lazy val csep: Parser[String] =
    """,""".r withFailureMessage "Expected a comma"

  lazy val param: PackratParser[Parameter] =
    id ~ id ^^ { case ty ~ name => Parameter(name, ty) } withFailureMessage "Expected a given preceeded by its type"

  lazy val path: Parser[String] = """[\w/\.]+""".r

  lazy val include: PackratParser[Path] =
    "include" ~> commit(path) ^^ (p => Path(p)) withErrorMessage "Expected an include statement"

  lazy val includes: PackratParser[List[Path]] =
    ((rep1sep(include, seps) <~ seps).? ^^ { _.getOrElse(List()) }) withFailureMessage
      "Files can only begin with include statements"

  lazy val grepl_instruction: PackratParser[GREPLInstruction] =
    include ^^ { Include } | givens ^^ { Givens } | returns ^^ {
      Returns
    } | statement

  lazy val program: PackratParser[Program] =
    seps.* ~> commit(includes) ~ items <~ sep.* ^^ {
      case ins ~ is => Program(ins, is)
    }

  lazy val items: PackratParser[List[Item]] =
    repsep(item, seps)

  lazy val item: PackratParser[Item] =
    ("shape" ~> commit(shape)) |
      ("construction" ~> commit(construction)) withFailureMessage
      "Expected a shape or construction"

  lazy val shape: PackratParser[Shape] =
    id ~ sep ~ givens ~ sep ~ repsep(statement, sep) ~ sep ~ returns ^^ {
      case name ~ _ ~ givens_ ~ _ ~ states ~ _ ~ returns_ =>
        Shape(Construction(name, givens_, states, returns_))
    } withFailureMessage
      "Malformed shape"

  lazy val construction: PackratParser[Construction] =
    id ~ sep ~ givens ~ sep ~ statements ~ sep ~ returns ^^ {
      case name ~ _ ~ givens_ ~ _ ~ states ~ _ ~ returns_ =>
        Construction(name, givens_, states, returns_)
    }

  lazy val returns: PackratParser[List[Identifier]] =
    "return" ~> commit(
      repsep(id, csep) withFailureMessage "Expected a list of expressions")

  lazy val givens: PackratParser[List[Parameter]] =
    "given" ~> commit(
      ("points" ~> commit(ids) ^^ {
        _ map { Parameter(_, Identifier("point")) }
      } withErrorMessage
        "Expected a list of points")
        | rep1sep(param, csep)
    )

  lazy val statements: PackratParser[List[Statement]] =
    repsep(statement, sep) withFailureMessage "Malformed sequence of statements"

  lazy val statement: PackratParser[Statement] =
    positioned("let" ~> commit(commit(pattern) ~ "=" ~ commit(expr) ^^ {
      case p ~ "=" ~ e => Statement(p, e)
    } withFailureMessage "Malformed statement"))

  lazy val expr: PackratParser[Expr] =
    positioned(
      (exprNoDiff ~ "-" ~ commit(expr) ^^ {
        case e1 ~ "-" ~ e2 => Difference(e1, e2)
      }) | exprNoDiff
    )

  lazy val exprNoDiff: PackratParser[Expr] =
    positioned(
      ("{" ~> commit(repsep(expr, csep) <~ "}" ^^ { SetLit }) withErrorMessage "Malformed set literal") |
        (id ~ "(" ~ rep1sep(expr, csep) <~ ")" ^^ {
          case i ~ "(" ~ exprs => FnApp(i, exprs)
        }) |
        (id ^^ Exactly) withFailureMessage "Malformed expression")

  lazy val pattern: PackratParser[Pattern] =
    positioned(
      pattern_ins ^^ (pats => if (pats.length == 1) pats.head else Tuple(pats)))

  lazy val pattern_in: PackratParser[Pattern] =
    ("(" ~> commit(pattern_ins <~ ")") ^^ (patterns => Tuple(patterns))
      | id ~ "(" ~ commit(pattern_ins <~ ")") ^^ {
        case i ~ "(" ~ patterns => Destructor(i, patterns)
      }
      | id ^^ Id) withFailureMessage "Found a malformed pattern"

  lazy val pattern_ins: PackratParser[List[Pattern]] =
    rep1sep(pattern_in, csep)

  lazy val ids: PackratParser[List[Identifier]] =
    rep1sep(id, csep) withFailureMessage "Expected a comma-separated list of 1 or more identifiers"

  lazy val id: Parser[Identifier] =
    positioned("""[a-zA-Z_][\w_]*""".r ^^ Identifier) withFailureMessage "Expected an identifier"
}
