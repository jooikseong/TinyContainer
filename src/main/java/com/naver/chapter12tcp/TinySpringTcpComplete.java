package com.naver.chapter12tcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) @interface MyComponent {}
@Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.CONSTRUCTOR}) @interface MyAutowired {}

// 서비스 레이어(TCP로 들어온 데이터를 처리할 비즈니스 로직)
@MyComponent
class ChatService {
    public String processMessage(String message) {
        return "[Echo Service]" + message + " (Processed at " + new Date() + ") ";
    }
}

// TCP 서버 빈 (IoC에 의해 관리됨)
@MyComponent
class TinyTcpServer {
    private final ChatService chatService;
    private final int PORT = 8888;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    @MyAutowired
    public TinyTcpServer(ChatService chatService) {
        this.chatService = chatService;
    }

    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("--- Tiny TCP Server 시작 ( Port: " + PORT + ")");
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[Server] 클라이언트 연결됨: " + clientSocket.getInetAddress());

                    // 클라이언트 처리를 스레드풀에 위임 (비동기 처리)
                    threadPool.execute(() -> handleClient(clientSocket));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClient(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("[Server Received]" + inputLine);

                // 비즈니스 로직(Service) 호출
                String response = chatService.processMessage(inputLine);

                // 클라이언트에게 전송
                out.println(response);
            }

        }catch (Exception e) {
            System.out.println("[Server Error]" + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (Exception e) {

            }
        }
    }
}



public class TinySpringTcpComplete {
    public static void main(String[] args) throws InterruptedException {
        // 컨테이너 초기화 및 빈 등록 ( 수동 등록 시뮬레이션 )
        ChatService chatService = new ChatService();
        TinyTcpServer server = new TinyTcpServer(chatService);

        // TCP 구동
        server.start();

        // 테스트용 클라이언트 실행 시뮬레이션 (잠시 후 실행)
        Thread.sleep(1000);
        runTestClinet("Hello Tiny Spring!");
        runTestClinet("TCP Communication Test");
    }

    private static void runTestClinet(String s) {
        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 8888);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            )
            {
                out.println(s);
                System.out.println("[Client Received] " + in.readLine());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }
}
