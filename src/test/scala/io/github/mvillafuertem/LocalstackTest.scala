package io.github.mvillafuertem

import akka.stream.alpakka.sqs.scaladsl.{SqsPublishFlow, SqsSource}
import akka.stream.alpakka.sqs.{SqsPublishSettings, SqsSourceSettings}
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import io.github.mvillafuertem.LocalstackTest.LocalstackTestConfiguration
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.sqs.model.{Message, MessageAttributeValue, SendMessageRequest}

import java.nio.charset.StandardCharsets
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

final class LocalstackTest extends LocalstackTestConfiguration {

  it should "send a binary message" in {
    Source
      .fromIterator(() => (1 to 1).iterator)
      .map(_ =>
        SendMessageRequest
          .builder()
          .messageAttributes(
            Map(
              "Body" -> MessageAttributeValue
                .builder()
                .dataType("Binary")
                .binaryValue(SdkBytes.fromByteArray(java.util.Base64.getEncoder.encode("user:pass".getBytes(StandardCharsets.UTF_8))))
                .build()
            ).asJava
          )
          .messageBody("")
          .build()
      )
      .via(SqsPublishFlow(queue1Url, SqsPublishSettings.create())(awsSqsClient))
      .runWith(Sink.foreach(println))
      .futureValue shouldBe Done
  }

}

object LocalstackTest {

  trait LocalstackTestConfiguration
    extends LocalstackConfiguration
      with AnyFlatSpecLike
      with ScalaFutures
      with BeforeAndAfterEach
      with BeforeAndAfterAll
      with Matchers {

    implicit val defaultPatience: PatienceConfig =
      PatienceConfig(timeout = 60.seconds, interval = 200.millis)

        override protected def beforeAll(): Unit = {
          container.start()
          createQueue("queue1").futureValue.queueUrl() shouldBe queue1Url
        }

        override protected def afterAll(): Unit = {
          container.stop()
          awsSqsClient.close()
          system.registerOnTermination(container)
          system.registerOnTermination(awsSqsClient)
          system.terminate.futureValue
        }

    protected def createSqsSource(queueUrl: String): Source[Message, NotUsed] =
      SqsSource(
        queueUrl,
        SqsSourceSettings().withCloseOnEmptyReceive(true)
      )(awsSqsClient)

  }

}