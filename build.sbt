/* =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */


val kamonVersion        = "2.0.0-07ebec0a2acd1ecd22a6cf41cb770e8daf39e3cc"
val kamonScalaVersion   = "1.1.0-M1"
val kamonAkkaVersion    = "2.0.0-00ac32f97ca3e8e4aba98f37f48b66677d0a493f"

val akka25Version       = "2.5.13"

val kamonCore           = "io.kamon"                    %%  "kamon-core"            % kamonVersion
val kamonTestkit        = "io.kamon"                    %%  "kamon-testkit"         % kamonVersion
val kamonScala          = "io.kamon"                    %%  "kamon-scala-future"    % kamonScalaVersion
val kamonAkka25         = "io.kamon"                    %%  "kamon-akka-2.5"        % kamonAkkaVersion

val kanelaScalaExtension  = "io.kamon"  %%  "kanela-scala-extension"  % "0.0.14"
val kanelaAgent       =  "io.kamon" % "kanela-agent" % "0.0.19-SNAPSHOT" changing()

val akkaActor25         = "com.typesafe.akka"           %%  "akka-actor"            % akka25Version
val akkaSlf4j25         = "com.typesafe.akka"           %%  "akka-slf4j"            % akka25Version
val akkaTestKit25       = "com.typesafe.akka"           %%  "akka-testkit"          % akka25Version
val akkaRemote25        = "com.typesafe.akka"           %%  "akka-remote"           % akka25Version
val akkaCluster25       = "com.typesafe.akka"           %%  "akka-cluster"          % akka25Version
val akkaSharding25      = "com.typesafe.akka"           %%  "akka-cluster-sharding" % akka25Version

val protobuf            = "com.google.protobuf"         % "protobuf-java"           % "3.4.0"

parallelExecution in Test in Global := false

lazy val `kamon-akka-remote` = (project in file("."))
  .settings(noPublishing: _*)
  .aggregate(kamonAkkaRemote25)


lazy val kamonAkkaRemote25 = Project("kamon-akka-remote-25", file("kamon-akka-remote-2.5.x"))
  .settings(Seq(
    bintrayPackage := "kamon-akka-remote",
    moduleName := "kamon-akka-remote-2.5",
    resolvers += Resolver.mavenLocal
  ))
  .enablePlugins(JavaAgent)
  .settings(javaAgents += kanelaAgent % "compile;test")
  .settings(
    libraryDependencies ++=
      compileScope(akkaActor25, kamonCore, kamonAkka25, kamonScala, akkaRemote25, akkaCluster25, kanelaAgent) ++
        providedScope(akkaSharding25, aspectJ) ++
        optionalScope(logbackClassic) ++
        testScope(akkaSharding25, scalatest, akkaTestKit25, akkaSlf4j25, logbackClassic, kamonTestkit, kanelaAgent))
