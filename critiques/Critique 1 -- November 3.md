_Anna Pinson_

First off - this is the second week in a row that doing a critique slipped my mind, and I have no excuse that holds water.

I love geometry, but it's been so long that I might not be that helpful as far as knowing what implementations may be useful for a... geometric mathemetitian? Geometrist?

"When we choose a point on a line not just any point will do. I think there are some implicit preferences."
The first thing that comes to mind is the midpoint, which you showed how to find with your sample program.
I don't know how useful having this as a built-in thing would be.
The second thing that comes to mind is choosing points on the circle based on an input angle, which, again, may not be useful or may not be in the scope of your domain?

I feel that enumerable and non-enumerable sets of points should be different types, just because it seems like they have different... maybe not functionality per se, but
your DSL probably needs to handle them differently.

When would a user need to intersect a line with itself? It may not be accurate, but it's probably enough for this implementation to have this loci model and 
not allow users to intersect a line/circle with itself.

It's my understanding that loci may be non-enumerable, while points are always enumerable. I could see a point-set type that may be either, 
where the intersection of two point-sets always produces an enumerable point-set. I'm not sure if this would make implementaiton easier or harder, but from a user standpoint
it seems like this would require them to keep track of what is and isn't enumerable themselves, which probably isn't a big deal.

Conceptually, is there any major difference between points and loci? If loci can only intersect other loci, or there is some other implementation that's unique to just loci
(or some other implementation that would be useful to keep separate), then unifying them may make things more amgiuous.

Personally, I'm in favor of keeping them separate. Even though it may not be the case, it feels like points are simpler/more discrete, while loci are more complex/continuous,
and I'd like to define data constructors with this in mind.

To be honest, I don't have enough background to picture what you're visualizing for your intermediate layer. Perhaps this is something we can go over in class today.
