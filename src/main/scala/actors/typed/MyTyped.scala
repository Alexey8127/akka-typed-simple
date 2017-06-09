package actors.typed

import actors.typed.MyTyped.TypeMessage.{MessageToRespond, SimpleMessage}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.reflect.ClassTag


object MyTyped {

  sealed trait TypedMessage[+M]

  object TypeMessage {
    case class SimpleMessage[M](message: M) extends TypedMessage[M]
    case class MessageToRespond[M](message: M) extends TypedMessage[M]
  }

  trait TypedActor[A[+ _]] extends Actor {

    protected def call[B](c: A[B]): B

    override def receive: Receive = {
      case message =>
        message.asInstanceOf[TypedMessage[A[_]]] match {
          case SimpleMessage(m) =>
            call(m)
          case MessageToRespond(m) =>
            sender ! call(m)
        }
    }
  }

  case class ActorRefTyped[A[+ _]](ar: ActorRef) {

    import scala.concurrent.ExecutionContext.Implicits.global

    implicit val timeout = Timeout(10.seconds)

    /** it's used in case Command[Future[Result]] ===> Future[Result] **/
    def call[B](c1: A[Future[B]])(implicit ev: ClassTag[Future[B]], ev4: ClassTag[Int]): Future[B] = {
      import akka.pattern._
      (ar ? MessageToRespond(c1)).mapTo[Future[B]].flatMap(identity)
    }

    /** it's used in case Command[Result] ===> Future[Result] **/
    def call[B: ClassTag](c: A[B]): Future[B] = {
      import akka.pattern.ask
      (ar ? MessageToRespond(c)).mapTo[B]
    }

    /** convenient method for chaining methods**/
    def send[B: ClassTag](c: A[B]): ActorRefTyped[A] = {
      ar ! SimpleMessage(c)
      this
    }
  }

  def createTypedActor[A[+ _], B <: TypedActor[A] : ClassTag](actorSystem: ActorSystem): ActorRefTyped[A] = {
    val actor = actorSystem.actorOf(Props[B])
    ActorRefTyped[A](actor)
  }
}
