/* =========================================================================================
 * Copyright © 2013-2016 the kamon project <http://kamon.io/>
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

val kamonVersion        = "1.0.0"
val kamonScalaVersion   = "1.0.0"
val kamonAkkaVersion    = "1.0.0"

val akka24Version       = "2.4.20"
val akka25Version       = "2.5.8"

val kamonCore           = "io.kamon"                    %%  "kamon-core"            % kamonVersion
val kamonTestkit        = "io.kamon"                    %%  "kamon-testkit"         % kamonVersion
val kamonScala          = "io.kamon"                    %%  "kamon-scala-future"    % kamonScalaVersion
val kamonAkka25         = "io.kamon"                    %%  "kamon-akka-2.5"        % kamonAkkaVersion
val kamonAkka24         = "io.kamon"                    %%  "kamon-akka-2.4"        % kamonAkkaVersion

val akkaActor25         = "com.typesafe.akka"           %%  "akka-actor"            % akka25Version
val akkaSlf4j25         = "com.typesafe.akka"           %%  "akka-slf4j"            % akka25Version
val akkaTestKit25       = "com.typesafe.akka"           %%  "akka-testkit"          % akka25Version
val akkaRemote25        = "com.typesafe.akka"           %%  "akka-remote"           % akka25Version
val akkaCluster25       = "com.typesafe.akka"           %%  "akka-cluster"          % akka25Version
val akkaSharding25      = "com.typesafe.akka"           %%  "akka-cluster-sharding" % akka25Version

val akkaActor24         = "com.typesafe.akka"           %%  "akka-actor"            % akka24Version
val akkaSlf4j24         = "com.typesafe.akka"           %%  "akka-slf4j"            % akka24Version
val akkaTestKit24       = "com.typesafe.akka"           %%  "akka-testkit"          % akka24Version
val akkaRemote24        = "com.typesafe.akka"           %%  "akka-remote"           % akka24Version
val akkaCluster24       = "com.typesafe.akka"           %%  "akka-cluster"          % akka24Version

val protobuf            = "com.google.protobuf"         % "protobuf-java"           % "3.4.0"

resolvers += Resolver.mavenLocal

lazy val `kamon-akka-remote` = (project in file("."))
  .settings(noPublishing: _*)
  .aggregate(kamonAkkaRemote24, kamonAkkaRemote25)


lazy val kamonAkkaRemote25 = Project("kamon-akka-remote-25", file("kamon-akka-remote-2.5.x"))
  .settings(aspectJSettings: _*)
  .settings(Seq(
      bintrayPackage := "kamon-akka-remote",
      moduleName := "kamon-akka-remote-2.5",
      scalaVersion := "2.11.8",
      crossScalaVersions := Seq("2.12.1", "2.11.8")
  ))
  .settings(
      libraryDependencies ++=
        compileScope(akkaActor25, kamonCore, kamonAkka25, kamonScala, akkaRemote25, akkaCluster25) ++
        providedScope(aspectJ) ++
        optionalScope(logbackClassic) ++
        testScope(akkaSharding25, scalatest, akkaTestKit25, akkaSlf4j25, logbackClassic, kamonTestkit))

lazy val kamonAkkaRemote24 = Project("kamon-akka-remote-24", file("kamon-akka-remote-2.4.x"))
  .settings(aspectJSettings: _*)
  .settings(Seq(
      bintrayPackage := "kamon-akka-remote",
      moduleName := "kamon-akka-remote-2.4",
      scalaVersion := "2.12.1",
      crossScalaVersions := Seq("2.11.8", "2.12.1")
  ))
  .settings(
      libraryDependencies ++=
        compileScope(akkaActor24, kamonCore, kamonAkka24, kamonScala, akkaRemote24, akkaCluster24) ++
        providedScope(aspectJ) ++
        optionalScope(logbackClassic) ++
        testScope(scalatest, akkaTestKit24, akkaSlf4j24, logbackClassic, kamonTestkit))

enableProperCrossScalaVersionTasks
