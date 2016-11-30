Akka Remote Integration   ![Build Status](https://travis-ci.org/kamon-io/kamon-akka.svg?branch=master)
==========================

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/kamon-io/Kamon?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

***kamon-akka-remote-2.3.x*** [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.kamon/kamon-akka-remote-23_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.kamon/kamon-akka-remote_2.11)

***kamon-akka-remote-2.4.x*** [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.kamon/kamon-akka-remote-24_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.kamon/kamon-akka-remote_2.11)


Kamon's integration with Akka comes in the form of two modules: `kamon-akka` and `kamon-akka-remote` that bring bytecode
instrumentation to gather metrics and perform automatic `TraceContext` propagation on your behalf.

Both the <b>kamon-akka</b> and <b>kamon-akka-remote</b> modules require you to start your application using the AspectJ
Weaver Agent. Kamon will warn you at startup if you failed to do so.
</p>

### kamon-akka-remote ###

* __Remote TraceContext Propagation__: This bit of instrumentation allows basic `TraceContext` information to be
propagated across the remoting channel provided by Akka. This hooks in the low level remoting implementation that ships
with Akka, which means it will propagate the `TraceContext` when using plain remoting as well as when using the Akka Cluster.

<p class="alert alert-warning">
If you are using Akka Remote 2.4 or Akka Cluster 2.4, please make sure that you are using the <b>kamon-akka-remote_akka-2.4</b>
artifact instead of the regular <b>kamon-akka-remote</b>.
</p>


