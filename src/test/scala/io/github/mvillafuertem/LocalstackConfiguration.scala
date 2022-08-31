package io.github.mvillafuertem

import akka.actor.ActorSystem
import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import com.github.matsluni.akkahttpspi.AkkaHttpClient
import org.testcontainers.containers
import org.testcontainers.containers.wait.strategy.Wait
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{CreateQueueRequest, CreateQueueResponse, QueueAttributeName}

import java.io.File
import java.net.URI
import scala.compat.java8.FutureConverters._
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

trait LocalstackConfiguration {

  private val uri = "http://localhost:4566"
  private val accessKey = "accessKey"
  private val secretAccessKey = "secretAccessKey"
  private val region: Region = Region.US_EAST_1
  private val credentialsProvider = StaticCredentialsProvider
    .create(AwsBasicCredentials.create(accessKey, secretAccessKey))

  implicit val system: ActorSystem = ActorSystem()
  private val asyncHttpClient: SdkAsyncHttpClient = AkkaHttpClient.builder().withActorSystem(system).build()

  protected val queue1Url = "http://localhost:4566/000000000000/queue1"

  implicit val awsSqsClient: SqsAsyncClient = SqsAsyncClient
    .builder()
    .credentialsProvider(credentialsProvider)
    .endpointOverride(URI.create(uri))
    .region(region)
    .httpClient(asyncHttpClient)
    .build()

  protected def createQueue(queueName: String, fifo: Boolean = false): Future[CreateQueueResponse] =
    awsSqsClient
      .createQueue(
        CreateQueueRequest
          .builder()
          .queueName(queueName)
          .attributes(
            Map(
              QueueAttributeName.FIFO_QUEUE -> fifo.toString,
              QueueAttributeName.DELAY_SECONDS -> "0",
              QueueAttributeName.MESSAGE_RETENTION_PERIOD -> "86400",
              QueueAttributeName.CONTENT_BASED_DEDUPLICATION -> fifo.toString
            ).asJava
          )
          .build()
      )
      .toScala

  val container: containers.DockerComposeContainer[_] =
    DockerComposeContainer(
      new File(s"src/test/resources/docker-compose.it.yml"),
      exposedServices = Seq(ExposedService("localstack", 4566, 1, Wait.forLogMessage(".*plugin localstack.hooks.on_infra_ready:initialize_health_info is disabled.*", 1))),
      identifier = "docker_infrastructure"
    ).container

}
