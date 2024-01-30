package com.lifo.sync.rpc

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.web3j.protocol.Web3jService
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.ipc.{UnixIpcService, WindowsIpcService}

class Web3jConfig {
  def buildService(clientAddress: String): Web3jService = {
    var web3jService: Web3jService = null
    if (clientAddress == null || clientAddress == "") web3jService = new HttpService(createOkHttpClient)
    else if (clientAddress.startsWith("http")) {
      web3jService = new HttpService(clientAddress, createOkHttpClient, false)
    } else if (System.getProperty("os.name").toLowerCase.startsWith("win")) {
      web3jService = new WindowsIpcService(clientAddress)
    } else {
      web3jService = new UnixIpcService(clientAddress)
    }
    web3jService
  }

  private def createOkHttpClient = {
    val builder = new OkHttpClient.Builder
    configureTimeouts(builder)
    builder.build
  }

  private def configureTimeouts(builder: OkHttpClient.Builder): Unit = {
    val tos = 300L
    if (tos != null) {
      builder.connectTimeout(tos, TimeUnit.SECONDS)
      builder.readTimeout(tos, TimeUnit.SECONDS) // Sets the socket timeout too

      builder.writeTimeout(tos, TimeUnit.SECONDS)
    }
  }
}
