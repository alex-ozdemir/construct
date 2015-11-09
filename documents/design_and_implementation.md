# Language design and implementation overview

## Language design

### The fundamentals

The language inherits its fundamentals from geometry - all the objects are set
of points. Examples of such sets of points include:

   1. A single point
   2. A circle
   3. A line
   4. A segment
   5. A ray
   6. An enumerated union of some number of the above.

The fundamental verbs are also inherited from geometry. We can choose points
from sets, take the intersection of different sets, and construct circles,
lines, rays, and segments from points according to the small set of rules
described by Euclid in _The Elements_.

Interestingly, the intersection of two points sets does not only depend on the
types of those sets. For example, the intersection of two circles can be 0
points, 1 point, 2 points, or an entire circle (an infinite set of points).

This makes it hard to break each of the fundamental nouns into different types,
and then figure out the types of their intersections without actually running
the program.

In response to this I've been implementing the language as a dynamically typed
language, where all the fundamental nouns have their own types, but those can
only be determined at runtime. This makes me sad inside because I really like
[strong static type systems](www.rust-lang.org) and think they can do a lot to
help programmers write good programs.

This is a big deviation from my original idea where there was a more robust
static type system. I think the deviation is appropriate because it is driven
by geometric concerns.

tl; dr: think Python.

### Fundamentals of Composition

How do we do functions?

First question: Why do we want functions?

We want functions so that users don't have to repeat themselves. So that they
can do a construction once and never again. In the context of classical
geometry they need not be fancy - recursion isn't necessary.

But there's also another requirement. Somewhere in the back of my mind is the
end game goal - a language that allows for the interactive construction of
geometric drawings. One in which the computer can make suggestions to the human
of what they might want to do.

This means that we can to be able to understand our functions. Really. We have
to be able to understand what they do in such a way that we can simulate them
and then make suggestions to the user. If this is interactive then we should be
able to update the index of extant functions on the fly.

All in all, this makes me shy away from having our functions be Scala
functions. If they were, that would take advantage of the internals of our DSL
playing nicely with Scala, but it would force us to compile the external DSL to
Scala, to somehow index our functions, to simulate them - it just seems like a
mess. In writing this now I start to see ways that I could do it, but currently
I say no.

The language will have its own functions, rigid, that provide exactly what is
needed and no more. These functions (and all code) will be evaluated by an
interpreter rather than being compiled to Scala.

In the future this might change, but for now.

### The Engine

At the core of the language is a geometric engine which handles the fundamental
nouns and verbs of geometry. It should be like an internal DSL in Scala.

### Writing Programs

Doing an interpreted dynamically typed language allows for all sorts of ways of
writing programs. In particular I'm interested in building a whole-program
interpreter which you give a file and get back a drawing.

I'm also interested in building a REPL which will eventually grow into the
suggestion system I mentioned earlier.

### Program Output

A program should ultimately result in the language outputting a drawing. The
current version outputs a drawing using the `tkz-euclide` package, but I
imagine I'll write systems for other output formats later on.

One important note is that I think it is important to provide a single point of
communication between the interpreter and the output systems. Right now the
interpreter maintains a detailed list of all object that have been drawn, as
well as all the information required to draw them in a sort of AST.

This AST might need to be expanded in the future to accomadate a wider variety
of output systems.

### Other DSLs

As mentioned before, the big players in this space currently are the
`tkz-euclide` LaTeX package and the various graphical geometry tools.

I draw a lot of inspiration from `tkz-euclide`. I think it works quite well and
is just hard to use.

### Errors

Error handling right now is in a pretty bad state. I envision the interpreter
will throw errors that have geometric or syntactical meaning for the user and
that those error will cancel the last input to the REPL or will halt the
interpreter on a file.

## Language implementation

### Runtime & Internal/External

As discussed above, I've chosen to interpret the language for now. Part of that
decision is making it an external DSL. I think a lot of the additional
processing I want to do (I.E. suggestions) will be much easier if I can lock
the language down into a specific AST I control explicitly.

### Host Language

I chose Scala. It's a good mix of functional programming that isn't to afraid
of side effects. I also considered Haskell but avoided it because I didn't
think I would be able to code as fast in it.

### Architecture

Right now there are a few key components

#### The Parser & AST

There is an abstract syntax tree representing the structure of a program, and
a parser which produces that tree from text

#### The Engine

There is a geometric engine which defines the fundamental geometric objects
and how to do intersections, etc. on them. This engine forms a sort of DSL
internal to Scala. This engine is used whenever the user takes and
intersection and is thus responsible for determining which operations are
valid, in a sense.

#### The Interpreter

There is an interpreter which reads an AST and executes the user-indicated
instructions. Throw some runtime errors for things like trying to intersect two
lines and get a point. The interpreter maintains a very complete AST of sorts
with enough information to make drawings, which it yields to an output system
upon request.

#### The `tkz-euclide` Output System

This contains knowledge of `tkz-euclide` and can produce images from
constructions using this knowledge.

#### The Toolchain

The toolchain has two components, a whole-file interpreter which interprets a
file and produces `tkz-euclide` output, and a REPL which doesn't do a lot right
now but will one day allow you do define constructions on the fly.

Both of these lean on the parser, interpreter, and output system.

