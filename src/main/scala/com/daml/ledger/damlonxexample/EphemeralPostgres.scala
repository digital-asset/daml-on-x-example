// Copyright (c) 2020 The DAML Authors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.damlonxexample

import java.io.StringWriter
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.time.Instant

import org.apache.commons.io.{FileUtils, IOUtils}
import org.slf4j.LoggerFactory

import scala.io.Source
import scala.util.control.NonFatal

case class PostgresFixture(jdbcUrl: String, tempDir: Path, dataDir: Path, logFile: Path)

trait EphemeralPostgres {

  private val logger = LoggerFactory.getLogger(getClass)

  private val IS_OS_WINDOWS: Boolean = sys.props("os.name").toLowerCase contains "windows"

  protected val testUser = "test"

  @volatile
  protected var postgresFixture: PostgresFixture = _

  protected def startEphemeralPg(): PostgresFixture = {
    logger.info("starting Postgres fixture")
    val tempDir = Files.createTempDirectory("postgres_test")
    val tempDirPath = tempDir.toAbsolutePath.toString
    val dataDir = Paths.get(tempDirPath, "data")
    val postgresConfPath = Paths.get(dataDir.toString, "postgresql.conf")
    val postgresPort = findFreePort()

    def runInitDb(): Unit = {
      val command = Array(
        pgToolPath("initdb"),
        s"--username=$testUser",
        if (IS_OS_WINDOWS) "--locale=English_United States" else "--locale=en_US.UTF-8",
        "-E",
        "UNICODE",
        "-A",
        "trust",
        dataDir.toAbsolutePath.toString.replaceAllLiterally("\\", "/")
      )
      val initDbProcess = Runtime.getRuntime.exec(command)
      waitForItOrDie(initDbProcess, command.mkString(" "))
    }

    def createConfigFile() = {
      // taken from here: https://bitbucket.org/eradman/ephemeralpg/src/1b5a3c6be81c69a860b7bd540a16b1249d3e50e2/pg_tmp.sh?at=default&fileviewer=file-view-default#pg_tmp.sh-54
      // We set unix_socket_directories to /tmp rather than tempDir
      // since the latter will refer to a temporary directory set by
      // Bazel which is too long (there is a limit on the length of unix domain
      // sockets). On Windows, unix domain sockets do not exist and
      // this option is ignored.
      val configText =
        s"""|unix_socket_directories = '/tmp'
            |shared_buffers = 12MB
            |fsync = off
            |synchronous_commit = off
            |full_page_writes = off
            |log_min_duration_statement = 0
            |log_connections = on
            |listen_addresses = 'localhost'
            |port = $postgresPort
      """.stripMargin

      Files.write(postgresConfPath, configText.getBytes(StandardCharsets.UTF_8))
    }

    def startPostgres() = {
      val logFile = Files.createFile(Paths.get(tempDirPath, "postgresql.log"))
      val command = Array(
        pgToolPath("pg_ctl"),
        "-o",
        s"-F -p $postgresPort",
        "-w",
        "-D",
        dataDir.toAbsolutePath.toString,
        "-l",
        logFile.toAbsolutePath.toString,
        //,
        "start"
      )
      val pgCtlStartProcess = Runtime.getRuntime.exec(command)
      waitForItOrDie(pgCtlStartProcess, command.mkString(" "))
      logFile
    }

    def createTestDatabase(): Unit = {
      val command = Array(
        pgToolPath("createdb"),
        "-h",
        "localhost",
        "-U",
        testUser,
        "-p",
        postgresPort.toString,
        "test"
      )
      val createDbProcess = Runtime.getRuntime.exec(command)
      waitForItOrDie(createDbProcess, command.mkString(" "))
    }

    try {
      runInitDb()
      createConfigFile()
      val logFile = startPostgres()
      createTestDatabase()

      val jdbcUrl = s"jdbc:postgresql://localhost:$postgresPort/test?user=$testUser"

      PostgresFixture(jdbcUrl, tempDir, dataDir, logFile)
    } catch {
      case NonFatal(e) =>
        deleteTempFolder(tempDir)
        throw e
    }
  }

  private def waitForItOrDie(p: Process, what: String): Unit = {
    logger.info(s"waiting for '$what' to exit")
    if (p.waitFor() != 0) {
      val writer = new StringWriter
      IOUtils.copy(p.getErrorStream, writer, "UTF-8")
      sys.error(writer.toString)
    }
    logger.info(s"the process has been terminated")
  }

  private def deleteTempFolder(tempDir: Path): Unit =
    FileUtils.deleteDirectory(tempDir.toFile)

  private def findFreePort(): Int = {
    val s = new ServerSocket(0)
    val port = s.getLocalPort
    // we have to release the port so postgres can use it
    // note that there is a small window for race, as the release of the port and giving it to postgres is not atomic
    // if this turns out to be an issue, we need to find an atomic way of doing that
    s.close()
    port

  }

  protected def stopAndCleanUp(tempDir: Path, dataDir: Path, logFile: Path): Unit = {
    logger.info("stopping and cleaning up Postgres")
    val command = Array(
      pgToolPath("pg_ctl"),
      "-w",
      "-D",
      dataDir.toAbsolutePath.toString,
      "-m",
      "immediate",
      "stop"
    )
    val pgCtlStopProcess = Runtime.getRuntime.exec(command)

    try {
      waitForItOrDie(pgCtlStopProcess, command.mkString(" "))
    } catch {
      case ie: InterruptedException =>
        println(s"waitForItOrDie was interrupted at ${Instant.now().toString}!")
        println("postgres log:")
        Source
          .fromFile(logFile.toFile)
          .getLines()
          .foreach(s => println(s)) //otherwise getting wart (Any)
        throw ie
    }
    deleteTempFolder(tempDir)
  }

  private def pgToolPath(c: String): String = s"$c"

}
