package io.hhplus.tdd;


import io.hhplus.tdd.point.PointController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 동시성 테스트
 */
public class ThreadSafetyTest {
    private PointController _pointController = new  PointController();
    private long _id = 12345;

    @Test
    @DisplayName("동시에 충전/사용을 할 경우")
    public void ThreadTest_ChargeUse() throws InterruptedException {
        //100번을 반복
        int taskCount = 100;
        //쓰레드풀은 임의로 4로 둠 => 각자 컴퓨터 코어수에 따라 다름
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            //n개의 쓰레드가 동시에 충전/사용
            for (int count = 0; count < taskCount; count++) {
                tasks.add(() -> {
                    _pointController.charge(_id, 1000);
                    _pointController.use(_id, 1000);
                    return null;
                });
            }
            List<Future<Void>> futures = executor.invokeAll(tasks);

            //invokeAll로는 에러를 체그하지 않으므로 따로 체크하도록 함
            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException | InterruptedException e) {
                    e.getCause().printStackTrace();
                    fail("작업 중 예외 발생: " + e.getCause().getMessage());
                }
            }
        }finally {
            executor.shutdown();
        }

        //결과값 확인
        assertThat(_pointController.point(_id))
                .usingRecursiveComparison()
                .ignoringFields("updateMillis")
                .isEqualTo(new UserPoint(_id, 0, 0));
    }

    @Test
    @DisplayName("반복적으로 API를 호출했을 경우 테스트")
    public void ThreadTest_DuplicateCall() {
        long id = new Random().nextInt(Integer.MAX_VALUE);
        long amount = new Random().nextInt(Integer.MAX_VALUE);

        PointController pointController = new PointController();

        //빠르게 두번을 호출한다
        pointController.charge_duplicatecall(id, amount);

        //1초 이내에 두번 호출시 에러
        assertThatThrownBy(()->pointController.charge_duplicatecall(id, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("중복된 요청입니다.");

        //빠르게 두번을 호출한다
        pointController.use_duplicatecall(id, amount);

        //1초 이내에 두번 호출시 에러
        assertThatThrownBy(()->pointController.use_duplicatecall(id, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("중복된 요청입니다.");
    }
}
