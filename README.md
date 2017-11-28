![Logo](construct.png)

# Web Version

If you're new to Construct, you should use the web version. It is hosted [here][host], and there is an introduction [here][intro].

# Desktop Version

If you're interested in working on Construct itself, it is often easier to test with the desktop version.
To run the desktop version of the project, install Scala and SBT, and then do:

```
sbt
> project constructJVM
> runMain construct.ConstructGREPL
```

Then try

```
// Click on two locations to get points A and B
let C1 = circle(A, B)
:s circle
let C2 = 0
:s intersection
let D, E = intersection(C1, C2)
let P = line(D, E)
let Q = segment(A, B)
let I = intersection(P, Q)
```

One can also use the whole-file processor with:

```
sbt 'runMain construct.ConstructC example.con'
```

[intro]: https://github.com/alex-ozdemir/construct/wiki/Introduction-to-Construct
[host]: https://www.cs.hmc.edu/~aozdemir/construct/
