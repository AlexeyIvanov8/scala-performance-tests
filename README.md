# Scala performance tests
This project created for test some performance aspects of Scala and show need inprovements. 

# Technical note
For tests I use JMH benchmarking framework.  

PC configuration.

Intel(R) Core(TM) i5-6600K CPU @ 3.50GHz up to 4.40GHz
DDR4, single channel, 16Gb, 2133MHz

# Reflection cache synchronization tests
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
  
  # Change concurrent map modification
  
  I'm view two option:
   - Collections.synchronizedMap(new WeakHashMap()), current I select this option as semantic equivalent old
     + \+ save semantic of removing unused values by GC
     - \- have some synchronization overhead
     
   - ConcurrentHashMap - the best solution that I find, but remove weak references require some additional work. I'm not handle it yet
     + \+ almost full parallelilizm
     - \- no handling weak references
     
 # Test results
