# Scala performance tests
This project created for test some performance aspects of Scala and show need inprovements. 

## Technical note
For tests I use JMH benchmarking framework.  

PC configuration.

CPU: Intel(R) Core(TM) i5-6600K CPU @ 3.50GHz up to 4.40GHz

RAM: DDR4, single channel, 16Gb, 2133MHz

## Reflection cache synchronization tests
Current time Scala reflection is not optimize for using in multithreading environment. Because work with inner reflection cache wrap by
locking and with intensive work(for example - JSON serialization/deserialization) threads forced to wait relase lock. Even if reflection 
data is saved in application cache inner cache whatever is used. But if try analyze as reflections work we'll find what synchronization 
need only for concurrent access to scalaToJava and javaToScala maps(inner reflections cache).


```
// there synchronyzed must be replace one of concurrent map modifications without doubts
def enter(j: J, s: S) = synchronized {
    // debugInfo("cached: "+j+"/"+s)
    toScalaMap(j) = new WeakReference(s)
    toJavaMap(s) = new WeakReference(j)
}
```


```
// more interesting example: 
// 1. toScalaMap get replace to concurrent map - is ok
// 2. enter(key, result) no problem even is that we do multiple invocation from several threads. Because value all the same(we compare only
// some ref type). And exists two method, toJava, that can invoke enter after get in other thread, by we care this.
// Conclusion: this synchronized also can be relaced with concurrent map
def toScala(key: J)(body: => S): S = synchronized {
    toScalaMap get key match {
      case SomeRef(v) =>
        v
      case _ =>
        val result = body
        enter(key, result)
        result
    }
  }
```
  
## Changes of concurrent map modifications
  
I'm view two option:
  - Collections.synchronizedMap(new WeakHashMap()), current I select this option as semantic equivalent old
    + \+ save semantic of removing unused values by GC
    - \- have some synchronization overhead
     
  - ConcurrentHashMap - the best solution that I find, but remove weak references require some additional work. I'm not handle it yet
    + \+ almost full parallelilizm
    - \- no handling weak references
     
## Test results
 
  My machine have 4 cores, and one of them work with OS and background programs, i.g. 3 cores is free. In this way 4 and more threads no have sensible effect in comparison with 3 threads, and all tests execute with 1 and 3 threads, except completeTest. If you are interesting the results for othre threads number - clone and run! 
 
 - completeTest1..8 - emulate real work with reflection. In my opinion, tish is must significant test.
 
 - simpleTest1,3 - tests with little parsing logic overhead.
 
 - naiveReflection1,3 - getting full reflection data each iteration for access to one field.
 
 - cachedReflection1,3 - access to one field with caching reflection.
 

| Test name         | 2.12.0-RC1:throughput | sync WeakHashMap:throughput | ConcurrentHashMap:throughput | threads | Unit   |
|-------------------|-----------------------|-----------------------------|------------------------------|---------|--------|
| completeTest1     | 145                   | 260                         | 274                          | 1       | ops/ms |
| completeTest2     | 183                   | 390                         | 494                          | 2       | ops/ms |
| completeTest3     | 153                   | 422                         | 663                          | 3       | ops/ms |
| completeTest4     | 106                   | 313                         | 769                          | 4       | ops/ms |
| completeTest5     | 98                    | 420                         | 694                          | 8       | ops/ms |
| simpleTest1       | 1052                  | 1720                        | 1714                         | 1       | ops/ms |
| simpleTest3       | 852                   | 2547                        | 4682                         | 3       | ops/ms |
| naiveReflection1  | 55                    | 88                          | 89                           | 1       | ops/ms |
| naiveReflection3  | 28                    | 36                          | 40                           | 3       | ops/ms |
| cachedReflection1 | 53548                 | 53472                       | 53036                        | 1       | ops/ms |
| cachedReflection3 | 59587                 | 60537                       | 60849                        | 3       | ops/ms |

## Running

After clone project you can select Scala version and run tests:

1. Open build.sbt and select Scala version(default is 2.12.0-RC1):
> val scalaVersionString = "2.12.0-RC1"

2. Move to project dir and run sbt:
> sbt clean compile jmh:run
