package es.unizar.urlshortener

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//class WebSocketTests {
//
//    @LocalServerPort
//    private var port: Int = 0
//
//    class MyWebSocketHandler(
//            private val latch: CountDownLatch,
//            private val list: MutableList<String>
//    ) : TextWebSocketHandler() {
//        lateinit var session: WebSocketSession
//
//        override fun afterConnectionEstablished(session: WebSocketSession) {
//            this.session = session
//        }
//
//        override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
//            list.add(message.payload)
//            latch.countDown()
//        }
//
//        fun sendMessage(text: String) {
//            session.sendMessage(TextMessage(text))
//        }
//    }
//
//    @Test
//    fun onOpen() {
//        val latch = CountDownLatch(2)
//        val list = mutableListOf<String>()
//        val handler = MyWebSocketHandler(latch, list)
//        val client: WebSocketClient = StandardWebSocketClient()
//
//        client.doHandshake(handler, "ws://localhost:$port/api/fast-bulk")
//
//        // Await connection establishment
//        latch.await(10, TimeUnit.SECONDS)
//
//        // Send message
//        handler.sendMessage("https://www.youtube.com/,0")
//
//        // Wait for the message to be received
//        latch.await(30, TimeUnit.SECONDS)
//
//        println(list)
//
//        // Assert that the expected substring is present in the list
//        val expectedSubstring = "https://www.youtube.com/,http://127.0.0.1:$port/6f12359f," +
//                "no_qr,no_error,URI de destino no validada todavia"
//        Assertions.assertTrue(list.any { it.contains(expectedSubstring) },
//                "The expected substring was not found in the list.")
//    }
//}
