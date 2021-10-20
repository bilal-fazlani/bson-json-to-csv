package com.bilalfazlani

import io.circe.Codec.AsObject
import sttp.client3.quick.*
import sttp.client3.circe.*
import scala.util.Try
import org.apache.maven.artifact.versioning.ComparableVersion

case class RepoResponse(tag_name: String) derives AsObject

enum VersionCheck:
  case UpdateAvailable(newVersion: String, intalledVersion: String)
  case NoUpdateAvailable
  case CouldNotCheckLatestVersion

object AppVersion {
  def check: VersionCheck = getCurrentLatest
    .map { latest =>
      val githubVersion = ComparableVersion(latest)
      val buildVersion = ComparableVersion(BuildInfo.version)
      val updateAvailable = githubVersion.compareTo(buildVersion) > 0
      if (updateAvailable) then
        VersionCheck.UpdateAvailable(latest, BuildInfo.version)
      else VersionCheck.NoUpdateAvailable
    }
    .getOrElse(VersionCheck.CouldNotCheckLatestVersion)

  private def getCurrentLatest: Option[String] =
    Try(
      quickRequest
        .get(
          uri"https://api.github.com/repos/bilal-fazlani/bson-json-to-csv/releases/latest"
        )
        .response(asJson[RepoResponse])
        .send(backend)
        .body
        .toOption
    ).toOption.flatten
      .map(_.tag_name)
}