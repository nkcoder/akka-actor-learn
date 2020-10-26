package demo.actor

/** Note that if the routees are sharing a resource, the resource will determine if increasing the
  * number of actors will actually give higher throughput or faster answers. For example if the
  * routees are CPU bound actors it will not give better performance to create more routees than
  * there are threads to execute the actors.
  *
  * Since the router itself is an actor and has a mailbox this means that messages are routed
  * sequentially to the routees where it can be processed in parallel (depending on the available
  * threads in the demo.actor.dispatcher). In a high throughput use cases the sequential routing could become a
  * bottle neck. Akka Typed does not provide an optimized tool for this.
  */
package object router {}
