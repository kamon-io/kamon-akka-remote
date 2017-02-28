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

val kamonVersion      = "0.6.6"
val akka23Version       = "2.3.13"
val akka24Version       = "2.4.16"

val kamonCore           = "io.kamon"                    %%  "kamon-core"            % kamonVersion
val kamonScala          = "io.kamon"                    %%  "kamon-scala"           % kamonVersion
val kamonAkka23         = "io.kamon"                    %%  "kamon-akka-2.3"        % kamonVersion
val kamonAkka24         = "io.kamon"                    %%  "kamon-akka-2.4"        % kamonVersion
val kamonTestkit        = "io.kamon"                    %%  "kamon-testkit"         % kamonVersion

val akkaActor23         = "com.typesafe.akka"           %%  "akka-actor"            % akka23Version
val akkaSlf4j23         = "com.typesafe.akka"           %%  "akka-slf4j"            % akka23Version
val akkaTestKit23       = "com.typesafe.akka"           %%  "akka-testkit"          % akka23Version
val akkaRemote23        = "com.typesafe.akka"           %%  "akka-remote"           % akka23Version
val akkaCluster23       = "com.typesafe.akka"           %%  "akka-cluster"          % akka23Version

val akkaActor24         = "com.typesafe.akka"           %%  "akka-actor"            % akka24Version
val akkaSlf4j24         = "com.typesafe.akka"           %%  "akka-slf4j"            % akka24Version
val akkaTestKit24       = "com.typesafe.akka"           %%  "akka-testkit"          % akka24Version
val akkaRemote24        = "com.typesafe.akka"           %%  "akka-remote"           % akka24Version
val akkaCluster24       = "com.typesafe.akka"           %%  "akka-cluster"          % akka24Version

lazy val `kamon-akka-remote` = (project in file("."))
    .settings(noPublishing: _*)
    .aggregate(kamonAkkaRemote23, kamonAkkaRemote24)

lazy val kamonAkkaRemote23 = Project("kamon-akka-remote-23", file("kamon-akka-remote-2.3.x"))
  .settings(aspectJSettings: _*)
  .settings(Seq(
      bintrayPackage := "kamon-akka-remote",
      moduleName := "kamon-akka-remote-2.3",
      scalaVersion := "2.11.8",
      crossScalaVersions := Seq("2.10.6", "2.11.8")))  
  .settings(
      libraryDependencies ++=
        compileScope(akkaActor23, kamonCore, kamonScala, kamonAkka23, akkaRemote23, akkaCluster23) ++
        providedScope(aspectJ) ++
        optionalScope(logbackClassic) ++
        testScope(scalatest, akkaTestKit23, akkaSlf4j23, logbackClassic))

lazy val kamonAkkaRemote24 = Project("kamon-akka-remote-24", file("kamon-akka-remote-2.4.x"))
  .settings(aspectJSettings: _*)
  .settings(Seq(
      bintrayPackage := "kamon-akka-remote",
      moduleName := "kamon-akka-remote-2.4",
      scalaVersion := "2.12.1",
      crossScalaVersions := Seq("2.11.8", "2.12.1")))    
  .settings(
      libraryDependencies ++=
        compileScope(akkaActor24, kamonCore, kamonScala, kamonAkka24, akkaRemote24, akkaCluster24) ++
        providedScope(aspectJ) ++
        optionalScope(logbackClassic) ++
        testScope(scalatest, akkaTestKit24, akkaSlf4j24, logbackClassic))

enableProperCrossScalaVersionTasks