package com.skn.benchmark

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

import com.skn.benchmark.ReflectionPerformanceBenchmark.BenchmarkState
import com.skn.mapper.ViewMapper
import com.skn.model.{Attribute, Client, Service, ViewItem}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import scala.util.Random
import scala.reflect.runtime.{universe => ru}

/**
  * Reflection performance benchmark for measure throughput of reflection operations.
  * Created by Sergey on 01.11.2016.
  */
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 4)
@Measurement(iterations = 4, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.Throughput))
class ReflectionPerformanceBenchmark {

  /**
    * Create random item(not full random, but enough for prevent optimisation)
    * @param state - benchmark state object
    * @return new Client item
    */
  def getRandomItem(state: BenchmarkState): ViewItem =
    Client("John", state.random.nextInt(), BigDecimal(100000),
        Service("payment",
            Attribute("date", LocalDateTime.now()) ::
            Attribute("amount", BigDecimal(state.random.nextLong())) :: Nil
        ) ::
        Service("some",
            Attribute("test", "Str") ::
            Attribute("otherClient", Client("Frank", 31, BigDecimal(900000),
              Service("fServ", Attribute("fAttr", 23L) :: Nil) :: Nil)) :: Nil
        ) :: Nil
    )

  /**
    * Next benchmarks approximate for my real code. I have CPU with 4 core, and because
    * test performance from 1 to 4 threads(in real performance grow only where we change
    * number of threads from 1 to 3, because exists
    * OS, browser etc.) and with 8 threads, for show effect of
    * concurrent lock.
    */
  @Benchmark
  def completeTest(state: BenchmarkState): String =
    state.mapper.startWrite(getRandomItem(state))

  @Threads(2)
  @Benchmark
  def completeTest2(state: BenchmarkState): String =
    state.mapper.startWrite(getRandomItem(state))

  @Threads(3)
  @Benchmark
  def completeTest3(state: BenchmarkState): String =
    state.mapper.startWrite(getRandomItem(state))

  @Threads(4)
  @Benchmark
  def completeTest4(state: BenchmarkState): String =
    state.mapper.startWrite(getRandomItem(state))

  @Threads(8)
  @Benchmark
  def completeTest8(state: BenchmarkState): String =
    state.mapper.startWrite(getRandomItem(state))


  /**
    * Tests with little parsing logic overhead. For 1 and 3 threads, you can change
    * number of threads across you CPU configuration.
    */
  def getSimpleRandomItem(state: BenchmarkState): ViewItem =
    Client("John", state.random.nextInt(), BigDecimal(100000), List[Service]())

  @Threads(1)
  @Benchmark
  def simpleTest1(state: BenchmarkState): String =
    state.mapper.startWrite(getSimpleRandomItem(state))

  @Threads(3)
  @Benchmark
  def simpleTest3(state: BenchmarkState): String =
    state.mapper.startWrite(getSimpleRandomItem(state))


  /**
    * Test reflection access to field without caching. Its shown that reflection must be cached.
    * Also, multithreading execution there are no have profit.
    */
  def doNaiveReflection(state: BenchmarkState): Int = {
    val client = getSimpleRandomItem(state)
    val mirror = ru.runtimeMirror(Client.getClass.getClassLoader)
    val nameDecl = ru.typeOf[Client].typeSymbol.info.decl(ru.TermName("age")).asTerm
    val clientMirror = mirror.reflect(client)
    clientMirror.reflectField(nameDecl).get.asInstanceOf[Int]
  }

  @Threads(1)
  @Benchmark
  def naiveReflection1(state: BenchmarkState, blackhole: Blackhole): Unit =
    blackhole.consume(doNaiveReflection(state))

  @Threads(3)
  @Benchmark
  def naiveReflection3(state: BenchmarkState, blackhole: Blackhole): Unit =
    blackhole.consume(doNaiveReflection(state))

  /**
    * Test cached reflection access to field.
    */
  @Threads(1)
  @Benchmark
  def cachedReflection1(state: BenchmarkState, blackhole: Blackhole): Unit = {
    state.nameDeclMirror.bind(getSimpleRandomItem(state))
    blackhole.consume(state.nameDeclMirror.get)
  }

  @Threads(3)
  @Benchmark
  def cachedReflection3(state: BenchmarkState, blackhole: Blackhole): Unit = {
    state.nameDeclMirror.bind(getSimpleRandomItem(state))
    blackhole.consume(state.nameDeclMirror.get)
  }
}

object ReflectionPerformanceBenchmark {

  @State(Scope.Thread)
  class BenchmarkState {
    val mapper = new ViewMapper
    val random = new Random(System.nanoTime())


    def prepareNameDeclMirror(): ru.FieldMirror = {
      val client = Client("test", 1, BigDecimal(1), List[Service]())
      val mirror = ru.runtimeMirror(Client.getClass.getClassLoader)
      val clientClassSymbol = mirror.classSymbol(Client.getClass)
      val nameDecl = ru.typeOf[Client].typeSymbol.info.decl(ru.TermName("age")).asTerm
      val clientMirror = mirror.reflect(client)
      clientMirror.reflectField(nameDecl)
    }

    val nameDeclMirror = prepareNameDeclMirror()
  }
}