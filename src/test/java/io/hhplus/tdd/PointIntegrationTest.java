package io.hhplus.tdd;

import io.hhplus.tdd.point.PointController;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


/**
 * 포인트 통합 테스트
 */
public class PointIntegrationTest {

    @Test
    @DisplayName("포인트 통합 테스트")
    public void PointTest()
    {
        long id =new Random().nextInt(Integer.MAX_VALUE);
        long useAmount = new Random().nextInt(Integer.MAX_VALUE);
        long chargedAmount = useAmount + new Random().nextInt(Integer.MAX_VALUE);

        PointController pointController = new PointController();

        //GetTest => 처음 get하므로 amount가 0이 나와야 한다
        assertThat(pointController.point(id))
                .usingRecursiveComparison()
                .ignoringFields("updateMillis")
                .isEqualTo(new UserPoint(id, 0, 0));

        //충전 테스트
        var chargedResult = pointController.charge(id, chargedAmount);

        //Charge했으므로 chargedAmount값이 나와야 한다
        assertThat(chargedResult)
                .usingRecursiveComparison()
                .ignoringFields("updateMillis")
                .isEqualTo(new UserPoint(id, chargedAmount, 0));

        //사용 테스트
        var usedResult = pointController.use(id, useAmount);
        //사용했으므로 chargedAmount - usedAmount값이 나와야 한다
        assertThat(usedResult)
                .usingRecursiveComparison()
                .ignoringFields("updateMillis")
                .isEqualTo(new UserPoint(id, chargedAmount - useAmount, 0));

        //결과값 비교 - Point 테스트
        //결과값이 usedResult때와 같아야한다
        var resultUserPoint = pointController.point(id);
        assertThat(resultUserPoint)
                .usingRecursiveComparison()
                .ignoringFields("updateMillis")
                .isEqualTo(resultUserPoint);

        //히스토리 테스트
        var resultHistory = pointController.history(id);
        List<PointHistory> exceptedHistory = List.of(
                new PointHistory(1, id, chargedAmount, TransactionType.CHARGE,0),
                new PointHistory(2, id, useAmount, TransactionType.USE,0)
        );

        assertThat(resultHistory)
                .usingRecursiveComparison()
                .ignoringFields("updateMillis")
                .isEqualTo(exceptedHistory);
    }
}
