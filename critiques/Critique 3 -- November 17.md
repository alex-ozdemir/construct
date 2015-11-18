_Anna Pinson_

You've done an _incredible_ job so far.

As a programmer, I prefer the `let A = intersection(B, C)`; however, I'm sure mathemeticians would appreciate using more natural language.

Maybe the most natural way for users to define things would be just to `define` them:

`define Triangle of Point a, Point b, Point c as {the?} Union of Segment a,b, Segment b,c, Segment a,c`.

I'm not sure about more complex definitions. Maybe have `as:` and then supply the definition, with each line separated by `and`s.

For example:

```
define Illuminati of Point a, Point b, Point c as:
    Triangle of Point a, Point b, Point c, and
    let M1 be the midpoint of a, b, and
    let M be the midpoint of M1, Point c, and
    Circle of M, c
```

That seems natural enough to me, but I'm not sure if it would divert the feel of the language too far away from the "proof-ish" style you have going.
I'm not sure how to prevent it from drawing M1 and M, though. Perhaps with a `show`/`hide` keyword option? (`show Triangle` or `hide M1` in a different call???)

What I don't know is how to specify "how the type works." What would you want to specify? How the new type intersects with other types?
Would you be able to rotate/scale objects? Would you need to; is this something that you ever see in classical geometry?

Another question is on ellipses (the shape, not the punctuation). Should users be able to define curved objects like these, or would there be a built-in implementation like with circles?
This probably depends on how ellipses are defined in classical geometry, if they even are. If not, would it be useful to include them?

As for suggestions, users should probably be able to type in a command to show suggestions for what objects they could make/what functions they could call with the data currently on the screen, which would be output textually.
A visual representation might show in red (or your favorite color) what the possiblities are, or let the user cycle through the possiblities (maybe with the arrow keys, if possible?), since this might result in a lot of options.
They could also specify only a subset of the data they want suggestions for.
