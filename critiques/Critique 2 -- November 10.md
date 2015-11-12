_Robin Pollak_

To start off, you are doing awesome work--Good questions, good self analysis, great speculation about advantages and disadvantages of various strategies/approaches. I definitely am having a hard time understanding some of your concepts but I'm just going to ask a lot of questions so I can get better at critiquing/understanding your project in the future!

## Questions I Have For You

What exactly is a locus in this context? I thought that a locus was just a point in space but if thats the case I'm not sure I understand how `Locus intersect Locus -> Locus`. Are we instead talking about a locus to mean an uncountably infinite set of points defined by some rule?

As to your discussion of functions and reusability I think that is key (and so do you, I think). I'm not sure, however, about your design choice to not incorporate recursive functions. In my brief experience with context-free (which, while certainly distinct, is not completely different than this project) I found that recursion provided to be one of the most powerful tools for reproducing images in slightly altered forms. _Why cut it out, when it should be relatively easy to implement?_

Are there significant sacrifices made by pushing the typing back to runtime? I get the need to do it, I'm just curious about the tradeoffs in terms of use cases. If you could somehow type at compile (or interpret?) time would that provide you with a greater ability to provide errors at a convenient time for the user?

How is the space that things are being drawn onto represented in your model? Is it a plane with (x,y) coordinates? If so, is it reasonable to model loci as functions? This would allow you to discuss intersections and unions in a really neat way.

## Answers I Have For You

We talked a little bit about defining new data types in person today and, thinking about it more, I really like your idea of supporting only the bare minimum in your language and allowing for extensibility through libraries. This raises the question of how you want to support libraries, though. 

I think you what you could do, which would be really cool, is support the definition of keywords that have specific rules which could be checked at compile/interpret time. Like a `Paralell` could be a set of 2 lines that do not intersect, then a `Paralellogram` could be a set of 2 `paralells` defined by the points of intersection, then a `Rectange` could be a `paralellogram` where the angles of intersection are 90 degrees and a `Rhombus` could be a `paralellogram` where all the sides are the same, then a `Square` would have to be both a `rectangle` and a `rhombus`. This kind of extensibility would allow for a lot of flexibility for the user while still being able to call things what they are.

Going back to the recursion thing, I think you should reconsider incorporating recursion.

As to your choice of using an interpreter I personally that thats completely fine. I don't really see any disadvantages to it, especially if youre pushing back typing to runtime. While I can imagine some advantages of compiling (I think a fair amount of optimization could be done of the drawing, probably) I also think much of that work is out of the scope of this project/your interests. I guess, depending on the scope you want this project to have, it would be nice to be able to speed things up but I doubt that there will be such a heavy work load on the program that the speed would be a huge concern. _I think that at this point the tradeoff for giving yourself more development time, as opposed to speeding up run time is very worth it_.

I hope my comments/questions are helpful and I also hope that as time goes on I'm gonna get more and more knowledgeable about your project/topics and I can provide better insight! Cheers
