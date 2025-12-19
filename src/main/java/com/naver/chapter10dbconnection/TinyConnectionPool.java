package com.naver.chapter10dbconnection;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// 가상의 DB 연결 클래스
class MockConnection {
    private final int id;

    public MockConnection(int id) {
        this.id = id;
        // 실제로는 여기서 드라이버 로드 및 네트워크 연결이 일어남 (매우 무거운 작업)
        try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    public void excuteQuery(String sql) {
        System.out.println("[DB-Conn-] " + id + "] 실행 중: " + sql);
    }

    @Override
    public String toString() { return "Connection-" + id;}
}

// 커넥션 풀 구현
class TinyDataSource {
    private final int poolSize;
    private final BlockingDeque<MockConnection> pool;
    private final AtomicInteger createCount = new AtomicInteger(0);

    public TinyDataSource(int poolSize) {
        this.poolSize = poolSize;
        this.pool = new LinkedBlockingDeque<>(poolSize);
        System.out.println("--- TinyDataSource 초기화 (Pool Szie: " + poolSize + ")---");

        // 초기 연결 생성 (Eager Initalization)
        for (int i = 0; i < poolSize; i++) {
            pool.add(new MockConnection(createCount.incrementAndGet()));
        }
    }

    // 연결 빌려오기
    public MockConnection getConnection() throws InterruptedException {
        MockConnection conn = pool.poll(5, TimeUnit.SECONDS);
        if(conn == null) {
            throw new RuntimeException("[Error] 연결 가능한 DB 커넥션이 없습니다! timeout");
        }
        System.out.println("[Pool] >>> " + conn + "대여됨 (남은 개수: " + pool.size() + ")");
        return conn;
    }

    // 연결 반납하기
    public void releaseConnection(MockConnection conn) {
        if(conn == null) {
            pool.offer(conn);
            System.out.println("[Pool] <<< " + conn + "반납됨 ( 남은 개수: " + pool.size() + ")" );
        }
    }
}

// 비즈니스 로직에서 사용
class DatabaseService {
    private final TinyDataSource dataSource;

    public DatabaseService(TinyDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void doWork(String taskName) {
        MockConnection conn = null;
        try{
            // 1. 풀에서 빌려옴
            conn = dataSource.getConnection();
            conn.excuteQuery("SELETE * FROM " + taskName);
            Thread.sleep(1000);
        }catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            dataSource.releaseConnection(conn);
        }

    }
}


public class TinyConnectionPool {
    public static void main(String[] args) throws InterruptedException {
        TinyDataSource dataSource = new TinyDataSource(2);
        DatabaseService service = new DatabaseService(dataSource);

        Runnable task = () -> service.doWork(Thread.currentThread().getName());

        System.out.println("\n--- 동시 요청 테스트 시작 ---");
        Thread t1 = new Thread(task, "Task-A");
        Thread t2 = new Thread(task, "Task-B");
        Thread t3 = new Thread(task, "Task-C");

        t1.start();
        t2.start();
        t3.start();

        t1.join(); t2.join(); t3.join();
        System.out.println("\n--- 모든 작업 완료 ---");
    }


}
