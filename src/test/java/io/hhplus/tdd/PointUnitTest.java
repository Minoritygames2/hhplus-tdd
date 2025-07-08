package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointController;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 포인트 유닛 테스트
 */
public class PointUnitTest {

    /**
     * 포인트 내역 조회
     */
    @Test
    @DisplayName("현재 포인트 취득")
    public void GetPointTest()
    {
        long id =new Random().nextInt(Integer.MAX_VALUE);
        UserPoint expected = new UserPoint(id, new Random().nextInt(Integer.MAX_VALUE), System.currentTimeMillis());

        UserPointTable mockUserPointTable = mock(UserPointTable.class);
        when(mockUserPointTable.selectById(id)).thenReturn(expected);

        PointController pointController = new PointController(null, mockUserPointTable);

        UserPoint resultUserPoint = pointController.point(id);
        assertThat(resultUserPoint).isEqualTo(expected);
    }

    /**
     * 포인트 내역 조회 실패
     * ID값이 음수일 경우
     */
    @Test
    @DisplayName("현재 포인트 실패 - ID값이 음수")
    public void GetPointTest_idMinus()
    {
        long id = - new Random().nextInt(Integer.MAX_VALUE);

        PointController pointController = new PointController();

        //id가 음수값이면 에러
        assertThatThrownBy(()->pointController.point(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id값은 음수가 될 수 없습니다.");
    }

    /**
     * 포인트 차지 테스트
     * int 범위 한정
     */
    @Test
    @DisplayName("포인트 충전 성공(INT)")
    public void ChargePointTest_Int()
    {
        long id = new Random().nextInt(Integer.MAX_VALUE);
        long amount = new Random().nextInt(Integer.MAX_VALUE);

        PointController pointController = new PointController();
        UserPoint chargeResult = pointController.charge(id, amount);

        //updateMills를 제외한 값 비교
        assertThat(chargeResult)
                .usingRecursiveComparison()
                .ignoringFields("updateMillis")
                .isEqualTo(new UserPoint(id, amount, 0));

        //updateMills는 0이 아닌 값인지 확인
        assertThat(chargeResult.updateMillis()).isGreaterThan(0);
    }

    /**
     * 포인트 차지 테스트 실패
     * ID값이 음수일 경우
     */
    @Test
    @DisplayName("포인트 충전 실패 - ID값이 음수")
    public void ChargePointTest_idMinus()
    {
        long id = - new Random().nextInt(Integer.MAX_VALUE);

        PointController pointController = new PointController();

        //id가 음수값이면 에러
        assertThatThrownBy(()->pointController.charge(id, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id값은 음수가 될 수 없습니다.");
    }

    /**
     * 포인트 차지 테스트
     * 음수범위의 경우 에러처리
     */
    @Test
    @DisplayName("포인트 충전 실패 - 음수")
    public void ChargePointTest_Minus()
    {
        long id = new Random().nextInt(Integer.MAX_VALUE);
        long amount = - new Random().nextInt(Integer.MAX_VALUE);

        PointController pointController = new PointController();

        //amount가 음수값이면 에러
        assertThatThrownBy(()->pointController.charge(id, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("충전 포인트는 0보다 커야 합니다.");
    }

    /**
     * 포인트 차지 테스트
     * 최대 저장 범위를 벗어났을 경우 에러처리
     */
    @Test
    @DisplayName("포인트 충전 실패 - 충전 금액이 최대 저장 범위를 벗어남")
    public void ChargePointTest_OverFlow()
    {
        long id = new Random().nextInt(Integer.MAX_VALUE);
        long amount = new Random().nextInt(Integer.MAX_VALUE);

        UserPoint chargedUserPoint = new UserPoint(id, Long.MAX_VALUE, System.currentTimeMillis());

        UserPointTable mockUserPointTable = mock(UserPointTable.class);
        when(mockUserPointTable.selectById(id)).thenReturn(chargedUserPoint);

        PointController pointController = new PointController(null, mockUserPointTable);

        //기존 충전된 금액과 amount의 합이 maxValue보다 크면 에러
        assertThatThrownBy(()->pointController.charge(id, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("충전 결과가 long 범위를 초과했습니다.");
    }

    /**
     * 포인트 사용 테스트
     */
    @Test
    @DisplayName("포인트 사용 성공")
    public void UsePointTest()
    {
        long id = new Random().nextInt(Integer.MAX_VALUE);

        //사용하는 값보다 charge가 더 크게 하기위해 사용값 + rand
        long useAmount = new Random().nextInt(Integer.MAX_VALUE);
        long chargedAmount = useAmount + new Random().nextInt(Integer.MAX_VALUE);

        UserPoint chargedUserPoint = new UserPoint(id, chargedAmount, System.currentTimeMillis());
        UserPoint expectedPoint = new UserPoint(id, chargedAmount - useAmount , System.currentTimeMillis());

        //임시로 저장된 값을 사용해야하므로 mock을 사용한다
        UserPointTable mockUserPointTable = mock(UserPointTable.class);
        when(mockUserPointTable.selectById(id)).thenReturn(chargedUserPoint);
        when(mockUserPointTable.insertOrUpdate(id, chargedAmount - useAmount)).thenReturn(expectedPoint);

        PointHistoryTable mockHistoryTable = mock(PointHistoryTable.class);

        PointController pointController = new PointController(mockHistoryTable, mockUserPointTable);
        UserPoint resultAmount = pointController.use(id, useAmount);

        //updateMills를 제외한 값 비교
        assertThat(resultAmount)
                .usingRecursiveComparison()
                .ignoringFields("updateMillis")
                .isEqualTo(expectedPoint);
    }
    /**
     * 포인트 사용 테스트 실패
     * ID값이 음수일 경우
     */
    @Test
    @DisplayName("포인트 충전 실패 - ID값이 음수")
    public void UsePointTest_idMinus()
    {
        long id = - new Random().nextInt(Integer.MAX_VALUE);

        PointController pointController = new PointController();

        //id가 음수값이면 에러
        assertThatThrownBy(()->pointController.use(id, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id값은 음수가 될 수 없습니다.");
    }

    /**
     * 포인트 사용 테스트 실패
     * 현재 포인트보다 사용포인트가 많은 경우
     */
    @Test
    @DisplayName("포인트 사용 실패 - 현재포인트 < 사용포인트")
    public void UsePointTest_ExceedPoint()
    {
        long id = new Random().nextInt(Integer.MAX_VALUE);
        long amount = new Random().nextInt(Integer.MAX_VALUE);

        PointController pointController = new PointController();

        assertThatThrownBy(()->pointController.use(id, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("현재 포인트보다 사용 포인트가 많습니다");
    }

    /**
     * 히스토리 추출 테스트
     * 히스토리 추출 성공
     */
    @Test
    @DisplayName("포인트 내역 추출")
    public void GetHistoryTest()
    {
        long id = new Random().nextInt(Integer.MAX_VALUE);

        List<PointHistory> expectedHistory = new ArrayList<>();
        expectedHistory.add(new PointHistory(0, id, 1, TransactionType.CHARGE, System.currentTimeMillis()));

        PointHistoryTable pointHistoryTable = mock(PointHistoryTable.class);
        when(pointHistoryTable.selectAllByUserId(id)).thenReturn(expectedHistory);

        PointController pointController = new PointController(pointHistoryTable, null);
        assertThat(pointController.history(id)).isEqualTo(expectedHistory);
    }
}
