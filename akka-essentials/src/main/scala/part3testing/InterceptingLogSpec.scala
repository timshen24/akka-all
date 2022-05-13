package part3testing

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.language.postfixOps

class InterceptingLogSpec
  extends TestKit(ActorSystem("InterceptingLogsSpec"))
  with ImplicitSender
  with {

}

object InterceptingLogSpec {

}