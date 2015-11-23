# Preliminary evaluation

## The State of the Language

I want to lead off by saying that I'm very happy with the state of the
language. When I started the project I had a wide range of what I thought I
could get done, and I have well blown past the minimum viable product.

We'll tackle the project one part at a time.

First, the geometry engine. It feels good to use, and honest to the underlying
domain. When I was able to use it to make the PNG renderer I felt like I was
witnessing a true triumph of the engine's value.
I also think it has room for improvement. Some of the internals are really
messy, for example, the similarity of lines, rays, and segments is not captured
as well as I might like. I also feel that the `Union` type could be smoother -
getting it to interact well with `SingleLocus` would be nice.

Next, the interpreter. It is cool in what it can do, but the internals are
somewhat of a disaster. They've been getting better, but there is still a long
way to go. In particular, I think functions are handled poorly - the loader is
_really_ inefficient, as are functions calls.

Next, the output systems. They're okay. The PNG one is a bit sloppy, but the
code is written reasonably well and it makes a lot of sense. The TkzEuclide one
is much worse, largely because the interpreter has changed so much. I should
take another look at it and try to get it to output short, human
readable/editable code.

Finally, the toolchain. I like the Graphical REPL a lot, and I think it will be
critical in the next steps. I would like to make it a bit more interactive.
Suggestions will be part of that. 'Undo' should be as well.

## Critical Flaws

While I think the Interpreter and Loader (the thing that reads all the files
and chases dependencies) are pretty messy right now, I don't think that they
are the biggest problem.

I think the biggest problem is that the language currently does not have a way
for the user to interact with types explicitly. I really want them to be able
to specify constructions that take certain types of objects, or to verify the
type of an object hey construct. We do this all the time in geometric writing
(" Let circles C1, C2 intersect at points A, B") and I think the language
should allow for it too.

## Vision for the Future

I started this project wanting to build a language for doing geometry, but also
wanting to build around that language to create some sort of interactive
construction assistance system. I think that is within my reach, and I'm going
for it!

Adding explicit type-checks will help with this (they will narrow the search
space), but then I need to design a good way for users to further narrow the
search space on their own.

## Reflecting on my Plan

I can't find the plan right now, which is weird given that I recall writing
one.

I have stuck fairly close to my initial plan, at least in the order of things.

The order was as follows:
   1. Make an engine. Lines, circles, points
   1. Switch to dynamic typing
   1. Write an output for LaTeX
   1. Write an interpreter
   1. Write a whole-file interpreter
   1. Write a REPL
   1. Functions!
   1. Loader
   1. Output in PNG
   1. Graphical REPL
   1. Better type system
   1. Custom Types.

It's been a long journey. I was surprised by how quickly I could do things in
the beginning, and was surprised by how long it took to fix the type system.

In retrospect, some of the simplifying assumptions I made to get function
support out super quickly ended up needing to be painfully un-assumed to get
the type system working well enough for custom types.

I have no beef with Scala (at least for this project). I think it mixes
functional and OO programming very well.

## Evaluation

To be honest I haven't done a lot of extra evaluation.

I am probably the only use of the language at this point. I think I'll iron out
my last two goals (Type checks and suggestions) and then start doing some user
tests to get more feedback (and hang out with some math majors <3).
