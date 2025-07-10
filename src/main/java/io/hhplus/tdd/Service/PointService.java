package io.hhplus.tdd.Service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

import static java.lang.Math.addExact;

public class PointService {
    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable userPointTable;

    //id별로 락을 별도 관리
    private final ConcurrentHashMap<Long, StampedLock> _locks = new ConcurrentHashMap<>();

    public PointService(PointHistoryTable historyTable, UserPointTable userPointTable) {
        this.pointHistoryTable = historyTable;
        this.userPointTable = userPointTable;
    }

    public UserPoint point(long id) {
        if (id < 0)
            throw new IllegalArgumentException("id값은 음수가 될 수 없습니다.");
        return userPointTable.selectById(id);
    }

    public List<PointHistory> history(long id) {
        if (id < 0)
            throw new IllegalArgumentException("id값은 음수가 될 수 없습니다.");
        return pointHistoryTable.selectAllByUserId(id);
    }

    public UserPoint charge(long id,long amount) {
        if (id < 0)
            throw new IllegalArgumentException("id값은 음수가 될 수 없습니다.");
        if (amount < 0)
            throw new IllegalArgumentException("충전 포인트는 0보다 커야 합니다.");

        //동시성을 위하여 lock 추가
        var lock = GetLock(id);
        var stamp = lock.writeLock();
        try{
            var nowUserPoint = userPointTable.selectById(id);

            //충전 결과가 long의 범위를 벗어나는지 체크
            long resultPoint;
            try {
                resultPoint = addExact(nowUserPoint.point(), amount);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("충전 결과가 long 범위를 초과했습니다.");
            }

            //포인트 충전
            var result = userPointTable.insertOrUpdate(id, resultPoint);
            //로그 저장
            pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
            return result;
        }finally {
            lock.unlock(stamp);
        }
    }

    public UserPoint use(long id,long amount) {
        if (id < 0)
            throw new IllegalArgumentException("id값은 음수가 될 수 없습니다.");

        //동시성을 위하여 lock 추가
        var lock = GetLock(id);
        var stamp = lock.writeLock();
        try
        {
            var nowUserPoint = userPointTable.selectById(id);
            var resultPoint = nowUserPoint.point() - amount;

            if (resultPoint < 0)
                throw new IllegalArgumentException("현재 포인트보다 사용 포인트가 많습니다");

            //포인트 사용 처리
            var result = userPointTable.insertOrUpdate(id, resultPoint);

            //로그 저장
            pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

            return result;
        }finally {
            lock.unlock(stamp);
        }
    }

    public UserPoint charge_duplicateCall(long id,long amount)
    {
        if(isDuplicateChargeRequest(id,  amount, TransactionType.CHARGE))
            throw new IllegalArgumentException("중복된 요청입니다.");
        return charge(id, amount);
    }

    public UserPoint use_duplicateCall(long id,long amount)
    {
        if(isDuplicateChargeRequest(id,  amount, TransactionType.USE))
            throw new IllegalArgumentException("중복된 요청입니다.");
        return use(id, amount);
    }

    /**
     * Lock 취득 - ID별로 락 관리
     */
    private StampedLock GetLock(long id)
    {
        return _locks.computeIfAbsent(id, k -> new StampedLock());
    }

    /**
     * 1000밀리세컨 이하로 같은 요청이 왔을 경우
     */
    private boolean isDuplicateChargeRequest(long id, long amount, TransactionType type)
    {
        return pointHistoryTable.selectAllByUserId(id).stream()
                .anyMatch(history -> history.type() == type
                        && history.amount() == amount
                        && System.currentTimeMillis() - history.updateMillis() <= 1000);
    }
}
