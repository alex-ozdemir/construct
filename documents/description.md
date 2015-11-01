# Project description and plan

## Motivation

The goal of the *construct* project is to build a language that allows humans
to articulate geometric constructions to computers in a natural way.

The general problem here is to design a way for humans to communicate geometry
to computers in a way which is both geometrically precise and easy for humans
to produce. I believe that a language is the correct solution because:

   - Language is generally a good solution to communication problems
   - The language used in geometric/mathematical writing is already reasonably
     easy to use and unambiguous.

I would classify a language that soles this problem as domain specific because
it will need to be highly tailored to the geometric domain in order to feel
easy to use. Furthermore, the domain requires none of the complex flow control
associated with general purpose languages.

## Language domain

The domain this language is targeted at is that of classical geometry (as set
forth by Euclid) on a euclidean plane.

This is the geometry governed by constructions of circles, lines, rays, and
segments (really just circles and rays) from points.

I imagine this language would be useful to people interested in doing classical
geometry (students and teachers of geometry classes) and might also end up
being useful to people interested in making geometric drawings.

## Language design

Geometry can be seen as the study of sets of points - some finite, some
uncountably infinite.

The *construct* language aims to provide a natural way of constructing both
finite and infinite sets of points from which drawings can be created.

A program would be a geometric construction - some sort of specification of
enumerable points and loci.

When the program runs the user should be able to get some idea of whether the
described construction is valid and should be able to get a drawing of it.

Control structures in the form of conditionals or loops probably won't make
much of an appearance. I don't expect recursion to be particularly valuable
either. However, non-recursive functions or sup-routines will be pretty
important. Once you've bisected a segment once you don't want to have to do it
every time.

There are two real ways for a user to write an invalid construction. The first
of these is by using invalid syntax, which will be handled traditionally
(parser throws a syntax error). The second is semantically (referring to an
undefined shape or taking an intersection that doesn't exist).
I expect that these will cause errors to be thrown when the program runs.

## Example computations

Some canonical examples are shown below:

### Find the midpoint of two points

```
Let A, B be points
Let C1 be a circle with center A including B
Let C2 be a circle with center B including A
Let the intersection of C1 and C2 be D, E
Let O be the intersection of segments DE and AB
O is the midpoint of A and B
```

Notice that the wording here is irregular - that is intentional. This shouldn't
be taken as an example of syntax, rather as an example of an idea that should
be expressible in the language.

### Bisecting an angle

```
Let A, B, C form an angle with vertex B
Choose a point D on ray BA
Let E be a circle with center B including D
Let E and ray BC intersect at F
Let G be the midpoint of D and F
The ray BG bisects the angle ABC
```

Again, this is just an example of an expressible idea.

One point of note is that this construction uses a prior construction - the
midpoint construction. This means our language should support [at least simple]
composition.

### Specify output

```
Create a drawing of this construction wiht black on tan.
label points A, B, C, G
```

Again, just an example of an expressible idea.

This gets across the idea that users should be able to control the output in
someway - the internal representation should not force a single type of output
drawing.
