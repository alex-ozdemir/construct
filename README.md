[notebook-fork]: https://github.com/hmc-cs111-fall2015/project-notebook/fork
[CS111-projects]: https://github.com/hmc-cs111-fall2015/hmc-cs111-fall2015.github.io/wiki/Project-links

![Logo](construct.png)

# Runbook

To run the project do

```
sbt 'runMain construct.ConstructGREPL'
```

Then try

```
given point A, point B
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
