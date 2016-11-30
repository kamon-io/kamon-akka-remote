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

import AspectJ._
import Settings._
import Dependencies._

lazy val kamonAkkaRemote23 = Project("kamon-akka-remote-23", file("kamon-akka-remote-2.3.x"))
  .settings(basicSettings: _* )
  .settings(crossScalaVersions := crossVersionAkka23)
  .settings(formatSettings: _*)
  .settings(aspectJSettings: _*)
  .settings(
      libraryDependencies ++=
        compileScope(akkaActor23, kamonCore, kamonAkka, akkaRemote23, akkaCluster23) ++
        providedScope(aspectJ) ++
        optionalScope(logback) ++
        testScope(scalatest, akkaTestKit23, akkaSlf4j23, logback))

lazy val kamonAkkaRemote24 = Project("kamon-akka-remote-24", file("kamon-akka-remote-2.4.x"))
  .settings(basicSettings: _* )
  .settings(crossScalaVersions := crossVersionAkka24)
  .settings(formatSettings: _*)
  .settings(aspectJSettings: _*)
  .settings(
      libraryDependencies ++=
        compileScope(akkaActor24, kamonCore, kamonAkka, akkaRemote24, akkaCluster24) ++
        providedScope(aspectJ) ++
        optionalScope(logback) ++
        testScope(scalatest, akkaTestKit24, akkaSlf4j24, logback))

def noPublishing = Seq(publish := (), publishLocal := (), publishArtifact := false)