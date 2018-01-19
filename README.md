# Akka Remote Integration<img align="right" src="https://rawgit.com/kamon-io/Kamon/master/kamon-logo.svg" height="150px" style="padding-left: 20px"/>

![Build Status](https://travis-ci.org/kamon-io/kamon-akka-remote.svg?branch=master)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/kamon-io/Kamon?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.kamon/kamon-akka-remote-2.5_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.kamon/kamon-akka-remote-2.5_2.12)

The <b>kamon-akka-remote</b> modules require you to start your application using the AspectJ
Weaver Agent. Kamon will warn you at startup if you failed to do so.
</p>

### Getting Started

Kamon akka module is currently available for Scala 2.10, 2.11 and 2.12.

Supported releases and dependencies are shown below.

| kamon-akka-remote-2.4  | status | jdk  | scala            | akka   |
|:------:|:------:|:----:|------------------|:------:|
|  1.0.0 | stable | 1.7+, 1.8+ | 2.11, 2.12  | 2.4.x |

| kamon-akka-remote-2.5  | status | jdk  | scala            | akka   |
|:------:|:------:|:----:|------------------|:------:|
|  1.0.0 | stable | 1.8+ | 2.11, 2.12  | 2.5.x |

To get started with SBT, simply add the following to your `build.sbt` or `pom.xml`
file:

```scala
libraryDependencies += "io.kamon" %% "kamon-akka-remote-2.5" % "1.0.0"
```

```xml
<dependency>
    <groupId>io.kamon</groupId>
    <artifactId>kamon-kamon-akka-2.5_2.12</artifactId>
    <version>1.0.0</version>
</dependency>
```

### kamon-akka-remote ###

* __Remote TraceContext Propagation__: This bit of instrumentation allows basic `Context` information to be
propagated across the remoting channel provided by Akka. This hooks in the low level remoting implementation that ships
with Akka, which means it will propagate the `Context` when using plain remoting as well as when using the Akka Cluster.


