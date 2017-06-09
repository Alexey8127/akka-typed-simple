## Synopsis

This is a simple implementation of typed akka actors. It's just a lightweight wrapper around untyped akka actor to work with actors in typesafe way. **Remember**: It's not production ready, I've made it just for fun.
## Code Example

The simple usage example looks like this and you can find it in source code:
```
  sealed trait Command[+Result]

  object Command {
    case class Add(x: Int) extends Command[Int]
    case class Mult(x: Int) extends Command[Future[Int]]
  }

  class TypedActorImpl extends TypedActor[Command] {
    var acc = 0

    override def call[B](c: Command[B]): B = c match {
      case Add(x) =>
        acc = acc + x
        acc
      case Mult(x) =>
        acc = acc * x
        Future.successful(acc)
    }
  }


  val actorSystem = ActorSystem()
  val calculatorActor = MyTyped.createTypedActor[Command, TypedActorImpl](actorSystem)
  val calculatorActor2 = MyTyped.createTypedActor[Command, TypedActorImpl](actorSystem)

  val sumResult = for {
      a <- calculatorActor
        .send(Add(2))
        .call(Mult(3))
      b <- calculatorActor2
        .send(Add(1))
        .send(Add(2))
        .call(Mult(3))
  } yield a + b

  sumResult.onComplete(println)

```
