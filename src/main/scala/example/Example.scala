package example

import actors.typed.MyTyped
import actors.typed.MyTyped.TypedActor
import akka.actor.ActorSystem
import akka.util.Timeout
import example.Example.Command.{Add, Mult}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

object Example extends App {

  sealed trait Command[+Result]

  object Command {
    case class Add(x: Int) extends Command[Int]
    case class Mult(x: Int) extends Command[Future[Int]]
  }

  /** do not use sender() explicitly in typed actor **/
  class TypedActorImpl extends TypedActor[Command] {
    var acc = 0

    override def call[B](c: Command[B]): B = c match {
      case Add(x) =>
        acc = acc + x
        Thread.sleep(100)
        acc
      case Mult(x) =>
        acc = acc * x
        Future.successful(acc)
    }
  }


  val actorSystem = ActorSystem()
  val calculatorActor = MyTyped.createTypedActor[Command, TypedActorImpl](actorSystem)
  val calculatorActor2 = MyTyped.createTypedActor[Command, TypedActorImpl](actorSystem)
  implicit val timeout: Timeout = Timeout(100.nanoseconds)

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

  Thread.sleep(100)

  actorSystem.terminate()
}
