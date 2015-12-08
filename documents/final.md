# Construct

## The Domain

Construct's domain is primarily classical geometry. The language aims to enable
those who desire to conceptualize and create diagrams for geometric
constructions.

In this way, the most likely users are students and teachers of geometry who
are trying to think through and draw constructions. To understand how their
needs are not being fully met, it is helpful to consider two solutions to the
problem.

### Graphical Construction Aides

There are many graphical applications which exist to help people do
constructions. One particular one is
[Sketchometry](http://start.sketchometry.org/), which is a web tool that allows
people to draw freeform shapes that are then snapped onto geometrically
significant ideas. Tools like this are awesome because they're both interactive
and easy to understand and use.

Unfortunately, they also have a few problems. One is that their only source of
input from the user is spatial (where the user draws a line or point). This
means that when the user provides spatial input that is close to two
geometrically distinct ideas (I.E. Bisector of an angle of a triangle and ray
from a triangle's angle through the midpoint of the opposite side) then it is
hard to disambiguate the two.

More seriously, most of these tools don't provide support for user-defined
functions or constructions. When we read geometric writing (or mathematical
writing) the users frequently reference ideas that they have previously
developed. For example, many constructions of a line parallel to some template
line through a given point rely on the construction for duplicating an angle.

For now we'll put both of these limitations in the back of our mind, along with
the strengths these tools show in terms of usability.

### Tkz-Euclide

There is also a singular LaTeX package I stumbled upon which allows for
geometric constructions to be done in LaTeX. That package is
[tkx-euclide](https://www.ctan.org/pkg/tkz-euclide?lang=en), which work inside
`tikz`. It allows users to textually specify the actions of a construction in a
precise geometric sense which mimics the tools of classical geometry.

This tool is awesome in how precise it is, but is hard to use. The code is
sometimes structured weirdly and has a heavy LaTeX accent. Furthermore, the
package is only documented in French :heart:. Because it is a LaTeX package,
the tool isn't really interactive - you write code and then have to compile the
document to see the results.

By virtue of living inside LaTeX the tool does allow for abstracting aways
constructions through composition using LaTeX macros, although it is a bit
weird.

### The State of the Domain

The various computer tools that address needs in this domain all have their own
strengths. By looking at those strengths, as well as the weaknesses, we can see
that ideally a language for classical geometry would have all the following
properties:

   - It should be easy to use - the user shouldn't have to type a lot to
     communicate a simple idea.
   - The language should feel like geometry. If textual it should read like
     geometric writing. If graphical, it should look like a construction
   - The language (and the systems around it) should be interactive.
     Constructions are simultaneously imagined and drawn.
   - The language should allow for geometric precision - you are articulating a
     geometric idea, not just a drawing. One should be able to imagine (if not
     implement) hooking a prover up to the language and letting it go.
   - The language should allow for abstraction of actions (functions), just
     like geometers refer to previous constructions.
   - The language should allow for abstraction of object (custom data types),
     just as a geometer might regard a triple of points as a triangle.
   - If possible - as a stretch goal the language should aide the user in doing
     constructions. This might mean helping the user search the space of
     possible actions they could take.

The Construct language sets out to exhibit the previous properties.

## The Design

### User Interace

Construct is a textual language. The user writes lines of text that correspond
to taking geometric actions (constructing a circle, line, or referencing an
earlier construction).

This text can be entered and processed in two ways. Text can be written to a
file and then processed by the whole-file processor which can output PNG and
LaTeX. Text can also be written into a Graphical Read Evaluate Print Loop which
displays an image of what has been constructed as the construction is built.
The construction can then be outputted as a Construct program, LaTeX, or PNG.

### Primitives

The language has a small set of type and action primitives. For types, there
are points, lines, segments, rays, circles, and unions (sets) of the above. For
actions one can take the union or intersection of any of the above types with
each other. Each line of a construction allows the user to perform some action
on some objects and then name the result. For example, the program below
results in this image:

```
given point A, point B
let C = circle(A, B)
```

![Just a circle](http://i.imgur.com/vlqU86y.png)

One can sequence these actions to make more complex constructions

```
given point A, point B
let C1 = circle(A, B)
let C2 = circle(B, A)
let D, E = intersection(C1, C2)
let L = line(D, E)
let S = segment(A, D)
let R = ray(B, E)
```

![Intersections](http://i.imgur.com/hlkv3KP.png)

While the essential component of the language is sequences of geometric
actions, the language allows for user-defined abstractions through both
user-defined constructions and their invocations (function definitions and
calls) and user-defined data types.

### Constructions

Constructions (functions) look like this:

```
construction use_bisect
given point A, point B
let P = perp_bisector(A, B)
return P

construction perp_bisector
given point A, point B
let C1 = circle(A, B)
let C2 = circle(B, A)
let D, E = intersection(C1, C2)
let L = line(D, E)
return L
```

![Perpendicular Bisector Function](http://i.imgur.com/S9X300n.png)

Notably, when a program is run, the first construction is interpretted at the
'Main' construction, and is assumed to take points as inputs. The interpretter
supplies it with those points (arbitrarily) and then runs it, possibly running
other constructions along the way. The intermediate results of constructions
that are invoked are not displayed.

### Custom Data Types

Users can also define new types of data, much in the way that a geometer might
define a triangle as being the union of the segments between three points.
Consider the following definition:

```
shape triangle
given point A, point B, point C
let a = segment(B, C)
let b = segment(A, C)
let c = segment(A, B)
return a, b, c
```

Shape definitions look a lot like constructions. The construction returns the
geometric object (in this case a set/union of segments) which represent how the
shape should intersect with other things. In this way, the following is a valid
construction:

```
construction my_intersection
given triangle T, line L
let I = intersection(T, L)
return I
```

(Although it could not be run on its own because its inputs are not points).

Importantly, shapes can also be destructed (pattern matched against) to recover
the arguments that were used to construct them. Thus, this is a valid
construction:

```
construction orthocenter
given triangle T
let triangle(A, B, C) = T
let a_b = perp_bisect(B, C)
let c_b = perp_bisect(A, B)
let I = intersection(a_b, c_b)
return I
```

and can be used like so (given that the above is saved in ortho.con)

```
include ortho.con
include triangle.con

construction do_me
given point A, point B
let T = obtuse_from_pair(A, B)
let M = orthocenter(T)
return
```

![Orthocenter](http://i.imgur.com/73FueLV.png)

Custom data types are important because they allow users to pass around objects
on the level of abstraction that is best for them. Geometers like to talk about
triangles, squares, and angles instead of juts ordered sets of points.

### Conditionals

It's worth noting that the language does not have any conditionals or branching
at the user level. Under the hood there is all sort of branching going on in
determining the intersections of various loci, but the user doesn't see that.

From their persepective a construction is a one-path thing.

### Error handling

Misuse of the language (Syntax errors, invalid pattern matching) will result in
errors. Most semantic errors are reasonable and the syntax ones are somewhat
cryptic. Some examples:

```
Construct $ given points A, B
Construct $ let D, E = A
construct.semantics.ConstructError: Error: Cannot match Basic(Point(0.0,0.0))
to pattern Tuple(List(Id(Identifier(D)), Id(Identifier(E))))

Construct $ let D, E = line(A, B)
construct.semantics.ConstructError: Error: Cannot match
Basic(Line(Point(0.0,0.0),Point(1.0,0.0))) to pattern
Tuple(List(Id(Identifier(D)), Id(Identifier(E))))

Construct $ le D = line(A, B)
[1.1] failure: Problem parsing statement

le D = line(A, B)
^

```

### The GREPL

This has been alluded to before, but the language also has a Graphical Read
Evaluate Print Loop, which includes a few meta-commands.

Most notable is the suggestion/search command. The user can invoke it with the
name of a construction to see how they could apply that construction to the
current set of objects. As an example:

```
Construct $ given points A, B
Construct $ include lib/lib.con
Construct $ let C = triangle_pt(A, B)
Construct $ :s circle
Construct $ :dt sug.png
```

![Suggestions](http://i.imgur.com/YhOrGN1.png)

### The vim syntax file

There is also a vim syntax file for the language in the project. It's not quite
an IDE, but it's fun! It is called `con.vim`, and you can find out how to
install it [here](http://vim.wikia.com/wiki/Creating_your_own_syntax_files).

## The Implementation

I implemented Construct as an external DSL in Scala. The big decision here was
to make it an external DSL. The reason I chose to do this was that I knew I
wanted to ultimately implement some sort of suggestion system, and I thought
this would be easier to do using an external DSL where I had direct control
over the AST and how things get executed.

I didn't have a lot of reason for choosing Scala over Rust/Haskell/Python
beyond the fact that I wanted to get to know Scala a bit better. Importantly I
knew that all of the above options had support for parser combinators (in some
form). I think Python would have been a bit of a pain because of the lack of
static typing, but it wouldn't have been a big problem.

### Architecture

The language has 5 key components:
   1. The Parser / Loader
   1. The Geometric Engine
   1. The Interpreter
   1. The Output Systems
   1. The Toolchain Systems

The last two are in plural case because their are multiple variants of each, as
will be explained.

On a high level, the parser parses the language, the engine solves geometric
problems such as computing intersections, the interpreter executes sequences of
actions and enforces the type system, the output systems render a dump of the
interpreter into a graphic, and the toolchain systems orchestrate all of the
above into a particular user interface.

#### The Parser / Loader

The parser is by far the simplest system. It is capable of parsing Construct
programs in their entirety. It also has entry points for parsing parts of a
construction for use in implementing an interactive user experience.

The loader is responsible for parsing files (recursively resolving their
dependencies) and returning the result as an AST. It's only real responsibility
is resolving dependencies, so I tend to lump it in with the input systems along
with the parser.

The parser and loader are controlled by the toolchain systems.

#### The Geometric Engine

The engine is a mathematically complex but architecturally simple system. It
defines a hierarchy of fundamental geometric types, as well as how they
interact with each other. The interpreter uses it to do the geometry required
in evaluating expressions.

Interestingly it is also used by the PNG output system to do some of the
geometry required in order to render lines and rays. This re-use in a different
part of the language says something good about the usefulness of this library
on its own.

#### The Interpreter

The interpreter is perhaps the most complex system. It tracks the state of a
construction as it is done. This means that after executing any statement of a
construction it maintains 3 particularly important data structures.

   1. A map from identifiers to type-tagged geometric objects
   1. A map from identifiers to constructions
   1. A map from identifiers to shape definitions

It uses these to evaluate expressions, match the results to patterns, and check
types during construction calls. In order to do function calls the interpreter
creates a new interpreter which it sets up with the actual paramters of the
function call. It also remove the called function from corresponding
(construction or shape constructor) function map to prevent recursion.
This is just one of the ways in which the interpreter is woefully inefficient.

#### The Output Systems

Construct currently has two output systems: a PNG renderer and a tkx-euclide
output system.

The PNG renderer takes a dump of information from the interpreter and produces
PNG images as a result. It is also capable of adding suggestions into the
image. That is, if the user has just requested suggestions from the GREPL, the
GREPL sends those suggestions to the PNG renderer, which draws them to an image
which gets displayed to the screen. The GREPL and ConcstructC (to be discussed
soon) both use the PNG renderer to output constructions as PNG images.

The tkz-euclide output system takes a dump of information from the interpreter
and uses it to output the construction as a drawing using the tkx-euclide LaTeX
package.

#### The Toolchain Systems

There are two ways users can run Construct programs.

The first of these is the ConstructC. This program takes in a Construct program
in a file and then interprets it, producing a PNG image and LaTeX document as
output.

The more interesting part of the toolchain is the Graphical Read Evaluate Print
Loop, which allows users to enter a program one line at a time, and see the
construction appear as they enter each statement. They can also undo
statements, ask for suggestions, and output their construction as a construct
program, a PNG image, or a LaTeX document.

### The AST

The language's Abstract Syntax Tree is fairly simple. Programs have includes
and then constructions / shapes. Constructions / shapes open with givens, then
have statements, then have returns. Statements bind a pattern to an expression.
Expressions are just nested function calls that can include both built-in
functions (intersection, circle, line, etc.) and user-defined function calls
(constructions, shape constructors)user-defined function calls (constructions,
shape constructors).

### Expression Evaluation

A lot of the interpreter's work is evaluating expressions. What exaclty does
that mean?

Expressions consist of function calls, with the arguments and results being
typed geometric objects and the functions being constructions or shape
constructors (which are just constructions that perform additional type
transformations). There are a number of built-in function and data types, and
how they interact is defined in a combination of the interpreter's logic and
the geometric engine's logic.

User defined construction are defined in terms of other functions, and can be
evaluated as such. The only extra computation that is done is that
constructions specify the type of their inputs and these types are then checked
by the interpreter during evaluation.

User defined data types (shapes) have two properties defined on them. First,
they have a geometric representation that is used fro computing intersections,
unions, etc. Second they have a way of being pattern matched against, which can
be used to break them apart into components (I.E. a triangle into 3 corners).
Custom data type are constructed using constructions that are evaluated as
normal, with an extra action taken at the end to transform the type.

### Dependencies

The Construct systems actually have relatively few dependencies. The parser
uses Scala's parser combinator library. The PNG renderer uses Java's 2D
Graphics library. The LaTeX output requires tkz-euclide in order to be
compiled.

Otherwise there aren't any dependencies. All the geometric logic is done
in-house.

## The Evaluation

### The State of the Language

I've been working on this project for a while, so it's good to take a chance to
think about what the language has grown into. The language feels very much like
the domain it was designed for. Composition and reference feel like they do in
classical geometry. The fundamental operations feel like those that you find in
classical geometry and behave as you would expect them to.

There are no structures provided for branching, looping, or recursion, but this
too feels honest to the domain. While you see branching in proofs, you rarely
(never?) see them in the constructions themselves.

Of course, everything isn't rosy. In particular, the classic geometric notion
of choosing some point from a set isn't particularly well represented (although
some of the structure has been built to support it). The concept of filtering
sets also isn't well represented (I.E., how to get a semicircle from a circle
and a diameter).

Nevertheless the language feels well suited to the domain - Construct is
undeniably a Domain Specific Language for Classical Geometry.

It's also good to look back at the goals I set, and see how well they've been
met.

   [ ] It should be easy to use - the user shouldn't have to type a lot to
       communicate a simple idea.
   [ ] The language should feel like geometry. If textual it should read like
       geometric writing. If graphical, it should look like a construction
   [x] The language (and the systems around it) should be interactive.
       Constructions are simultaneously imagined and drawn.
   [x] The language should allow for geometric precision - you are articulating a
       geometric idea, not just a drawing. One should be able to imagine (if not
       implement) hooking a prover up to the language and letting it go.
   [x] The language should allow for abstraction of actions (functions), just
       like geometers refer to previous constructions.
   [x] The language should allow for abstraction of object (custom data types),
       just as a geometer might regard a triple of points as a triangle.
   [ ] If possible - as a stretch goal the language should aide the user in doing
       constructions. This might mean helping the user search the space of
       possible actions they could take.

I think a lot of these goals have been might quite thoroughly. Some are more
ambiguous - is the language easy to use? Does it really feel like geometry? I
feel unqualified to answer these questions on my own. The final goal -
suggestions is partially implemented, but is a goal that expands infinitely -
one could always do suggestions in a better way.

### The Evaluation Process

How have evaluated my language? The honest answer is that I haven't evaluated
it very rigorously. Most of my judgements about it come out of the time I spent
this last week writing a few small libraries for it. As I wrote them I got a
bit of a feel for what the language was like (and found a bug or two along the
way).

I think to really know how well the language is meeting its goals I would need
to toss it at a few mathematicians and see how they feel about it.

That being said, while my own thoughts on the language may not be the most
reliable, they're worth something, so I'll share some of them now.

### Design Decisions (through anecdotes)

Design decisions are the business of building a programming language. For a
few, you think about them all the time. You ponder and try to find the best way
to solve a hard problem. For many more you don't think about them at all - they
emerge already made from your way of thinking. Perhaps later you notice them as
you start to get hints of their consequences.

Rather than listing a bunch of the decisions I made, I want to tell the story
of my language in three parts. I suspect that the design decisions will shine
through those stories.

#### The Type System

Before working on Construct (and while implementing it) I spent a bunch of time
working in Rust, Scala, and Haskell. All of these languages have static,
powerful type systems that they use to do incredible things.

And thus I started my project with a vision that it too would have a static
type system. Looking back at my first notebook entry I imagined a type system
in which there were two type of point sets, those that were finite and those
that were uncountable. These types could each be combined to produce objects of
the other type according to certain rules.

For example, the intersection of two circles would produce either 0, 1, or 2
points - in all cases a finite set of points. But this example has a fatal
flaw, the intersection of a of a circle with itself is the same circle, which
is not a finite set of points.

There are many other examples of intersections which break the type system I
had proposed, and some were a bit more subtle than intersecting a circle with
itself.

All and all, I had to let the static type system go, because it wasn't honest
to the geometry.

And so was born the dynamic type system. This model of the language was one in
which the interpreter was responsible for tracking the types of every geometric
object as the program ran. It could then use those types to do type-checking at
runtime. For example, when one constructs a circle from two inputs, those input
must both be points, and the interpreter was responsible for enforcing this.

At first this meant that the interpreter kept the points separate from
everything else - they lived in their own data structure. Whenever a locus was
produce it was put in this special data structure if it was a point, and in the
normal variable storage otherwise.

This allowed the interpreter to enforce that the basic type constructors (line,
ray, circle, etc.) received only points as their arguments, and also allowed
for function calls that didn't check the types of their arguments.
Essentially the interpreter divided the space of types into 'point' and
not-point'. Unfortunately this view of things wouldn't scale. If the user
wanted to define a custom data type which was constructed from segments, they
couldn't enforce that. If they wanted to make a construction which took a line,
a point, an returned a line through that point parallel to the given line, they
were out of luck.

The next step was to have the interpreter type-tag all of its variables. This
allowed for constructions that took and type-checked any type of argument. It
also allowed for custom data types to be defined and destructed into their
components.

This remains the current implementation of they type system. Interestingly,
this implementation is also one of the cleanest yet. The interpreter only holds
3 data structures, and the rules for how it evaluates expressions and does
pattern matches seem to be the most simple yet.

I think that this simplicity reflects the fact that this type system is well
suited to the underlying computation.

#### The Geometric Engine

I opened up the project with the knowledge that there would have to be a
geometry engine specifically suited to classical geometry behind the language,
powering all geometric computations.

For a brief while I entertained the idea of not having such an engine. My
language could be a purely declarative one, in which the result of the program
running is just a program in another language that can make the drawing. The
problem with this is that when doing classical geometry one wants to do things
like take the intersection of two circles and get the resulting points. But the
question is, how many points are there?

One could let the user guess, and faithfully compile to something like
tkz-euclide which will catch the error, but at this point we're pretty far away
from an interactive system. (That or we're compiling the LaTeX in real time,
which also seems like a problem).

Consequently, I chose to just write the geometric engine, with the goal of
making it not only useful for the interpreter of my language, but also general
geometric computations.

The engine ended up defining a number of subtypes of locus, and defining union,
intersection, and a few other operations on them. It feels pretty good to use,
and is very well tested (For most parts of the project I didn't do much
automated testing because the language was changing so fast. On the other hand,
for the engine I knew it would be pretty stable, and I didn't want it failing
me at any point, so I wrote lots of tests for it).

Making the engine valuable as a standalone library ended up being a good call.
Much later, when writing the PNG renderer I found myself needing to do a few
geometric computations during the rendering process, and was easily able to use
the geometric engine to do them.

#### The Output Systems

When I started the project I had the vision that a construction should be able
to be outputted in multiple forms. It should be possible to get a drawin for
your construction as an image, as LaTeX, but is should also be possible to come
in later and easily write code to produce a new type of output.

The first output form I did was tkz-euclide (the LaTeX package). In hindsight
this was an interesting place to start because tkz-euclide works nothing like
any other graphical tool I've ever seen. It is actually fully capable of doing
its own geometric computations (including the intersection of circles and
lines).

Looking back at my notebook I see that I commented at the time

>I think that trying to design some sort of super-redundant output AST for the
>construction would make writing different rendering systems much easier

which drives home the idea that outputting to tkz-euclide wasn't just drawing a
whole bunch of shapes, it was constructing a semantically meaningful idea in a
language with non-trivial syntax.

So I did it, I gave the tkz-euclide output system a slightly modified version
of the construction's AST, and it was able to produce output. Unfortunately, it
was not to last.

I added functions and the PNG renderer next. Adding the PNG renderer was an
entirely different experience because it really just is drawing a whole bunch
of shapes, without knowledge of the structure of the construction. It was a
whole different (and much simpler) beast than outputting tkz-euclide code.

Functions also made it really hard to output human-readable tkz-euclide code,
because the language doesn't support sub-rountines beyond LaTeX macros (which I
didn't pursue at the time). At the end of the day I ended up writing a pretty
hacky tkz-euclide output system.

Looking back what I see is that rendering PNG is really just drawing and image,
which can be easily done from a list of current geometric objects. On the other
hand, outputting tkz-euclide is much more complicated. Really I needed to write
a full transpiler - completely aware of how LaTeX works and ready to use macros
to faithfully translate the code.

The interesting thing here is that such a transpiler could run on its own,
without requiring the interpreter to actually run the Construct program. It
would be, in essence, a true transpiler, deferring the computation into another
programming language.

### Next Steps

Moving forward, there are some loose ends to clean up, along with a stretch
goal or two.

#### The `new` Problem

One problem is illustrated by this program:

```
given point A, point B
let C = circle(A, B)
let L = line(A, B)
let D, E = intersection(C, L)
```

![Interference](http://i.imgur.com/XiSsc6n.png)

The problem is that one of `D` or `E` is equivalent to `B`, but we don't have
any way of knowing which. Which it is is dependent on the order in which the
results of the intersection are pattern matched to `D, E`, which isn't known to
the user.

What the user really wants here is the intersection that isn't `B`. The way
I've accomplished this so far is by introducing a new built-in function `new`,
which filters out objects that already exist from a set of loci. In use it
looks like:

```
given point A, point B
let C = circle(A, B)
let L = line(A, B)
let D = new(intersection(C, L))
```

![New](http://i.imgur.com/pJ8vxeo.png)

This works out in this case, but is somewhat of an imprecise tool. What the
user really wanted to say is that they wanted the _intersection that is not B_,
not a _new intersection_.

I've toyed around with the idea of being able to take set differences, such as:

```
let D = intersection(C, L) - {B}
```

but now that I think about it I wonder about this style instead:

```
given point A, point B
let C = circle(A, B)
let L = line(A, B)
let D, B = intersection(C, L)
```

It definitely plays nicer with pattern matching, and seems to allow the user to
make assertions about their construction which is cool. The remaining question
is whether it is complete - does it allow users to express all that they want
to express?

At any rate, this 'newness' or 'otherness' problem needs to be resolved in an
elegant fashion.

### TkzEuclide

As mentioned before, the current output to tkz-euclide is pretty hacky. I would
like to revisit it and write a true transpiler, as described above.

### Error Handling

Right now, error handling is done, but some errors still slip through and crash
the GREPL. Furthermore, the parser errors are truly cryptic.

If this is going to be a real user-facing tool then I need to clean these up.

## The End?

At the end of the day, I'm happy with the progress that I've made. I think the
language turned out well, I think the experience was valuable to me, and I look
forward to getting this polished enough for users.
