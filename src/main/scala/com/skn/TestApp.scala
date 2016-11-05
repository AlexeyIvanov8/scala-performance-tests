package com.skn

import java.time.LocalDateTime
import java.util.concurrent._

import com.skn.mapper.ViewMapper
import com.skn.model.{Attribute, Client, Service}

import scala.util.Random

/**
  * Application, if you want view what mapper really do
  */
object TestApp extends App {

  override def main(args: Array[String]): Unit = {
    val mapper = new ViewMapper
    val random = new Random(System.nanoTime())
    val client = getClient(random)

    System.out.println("Raw client: " + client.toString)
    System.out.println("Write client: \n" + mapper.startWrite(client))
    System.out.println("Cache size 1 = " + mapper.reflectCache.size)
    mapper.startWrite(client)
    System.out.println("Cache size 2 = " + mapper.reflectCache.size)

    // home-made benchmark, you can set large number of iterations and batch size,
    // run application and leisurely fetch some profiling info: analyze threads dump,
    // run VisualVM etc.
    val executorService = new ThreadPoolExecutor(2, 2, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue[Runnable]())
    bench(executorService, 2, 4, 100000)
  }

  private def bench(executorService: ExecutorService, threads: Int, iterations: Int, batch: Int): Unit =
  {
    for(k <- 0 to iterations) yield {
      val bTime = System.nanoTime()
      val futures = for (i <- 0 until threads) yield executorService.submit(new Callable[Long] {
        override def call(): Long = {
          var count = 0L
          val viewMapper = new ViewMapper
          val random = new Random(System.nanoTime())
          for (j <- 0 until batch) yield {
            val client = getClient(random)
            val data = viewMapper.startWrite(client)
            count += (if(data.isEmpty) 0L else 1L)
          }
          count
        }
      })
      val res = futures.map(fut => fut.get()).sum
      val timeMs = (System.nanoTime() - bTime) / 1000000
      System.out.println("res = "+res + " time = " + timeMs)
      System.out.println(res + " items converted to data in " + timeMs + "ms, " + (res.toDouble / timeMs.toDouble) + "ops in ms")
    }
  }

  /**
    * Simple create test client item
    * @param random
    * @return
    */
  private def getClient(random: Random): Client =
    Client("John" + random.nextPrintableChar(), 29, BigDecimal(100000),
      Service("payment",
        Attribute("date", LocalDateTime.now()) ::
          Attribute("amount", BigDecimal(1000)) :: Nil
      ) ::
        Service("some",
          Attribute("test", "Str") ::
            Attribute("otherClient", Client("Frank", 31, BigDecimal(900000),
              Service("fServ", Attribute("fAttr", 23L) :: Nil) :: Nil)) :: Nil
        ) :: Nil
    )
}
