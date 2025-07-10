\[JAVA 동시성 제어 방식]

●Synchronized

&nbsp;- 특정 객체 또는 블럭에 대한 락을 획득함.

&nbsp;- JVM레벨에서 모니터락을 사용함

&nbsp;- 자바에서 제공해주는 기능으로 간단하고 사용하기 쉬움

&nbsp;- 한번에 하나의 스레드에서만 실행하도록 함

&nbsp; => 다른 스레드에서는 락이 풀릴때까지 대기

&nbsp; =>  병렬성이 낮아짐

&nbsp;- 상호 대기상태에 주의해야 함

&nbsp;- 세밀한 제어가 어려움

&nbsp;- 대기중 스레드 관리가 어려움

&nbsp;- 다른 제어방식에 비해 성능이 떨어짐

&nbsp; => 단순한 동기화에는 유리할 수 있음

●ReentrantLock

&nbsp;- 고급 락 API

&nbsp;- tryLock, isHeldByCurrentThread 등의 락과 관련된 다양한 기능을 이용할 수 있음

&nbsp;- 공정한 락 설정 가능

&nbsp; => 다른 락의 경우 락이 풀리면 그 순간 운좋게 접근한 스레드가 가져가지만, 공정한 락을 설정하면 요청 순서대로 락이 가능

&nbsp;- 사용법이 Synchronized보다 복잡함

&nbsp;- 수동으로 unlock을 해야함. 락이 해제되지 않으면 다음 스레드가 영원히 기다리게 되어 데드락이 발생할 수 있으ㅁ

●ConcurrentHashMap

&nbsp;- 동시성에 안전한 해시맵

&nbsp; => 위의 두 경우는 lock 방식이고, ConcurrentHashMap은 해시맵이다

&nbsp;- 높은 성능을 가짐

&nbsp;- get연산은 락없이 가능함. 

&nbsp;=> 읽기작업에 빠름

&nbsp;- key형식으로 락을 걸며 전체 Map에대한 락을 걸 수 없음

&nbsp;- null key, null value는 불가능함

&nbsp;- 병렬성이 과하면 성능이 떨어짐

●Atomatic

&nbsp;- 원자적으로 안전한 연산을 제공함(변수)

&nbsp;- 스레드간 공유되는 값의 변경을 안전하게 처리함

&nbsp;- compareAndSet(A,B)를 이용해 CAS(Compare And Swap)처리를 유용하게 할 수 있음

&nbsp;  => 메모리안의 동기화된 객체에 한하며 DB를 직접 비교하지는 못함(DB와 무관하게 메모리 내 원자성 보장)

&nbsp;  => if(compareAndSet(A,B))에 실패했을 경우 반복 호출 등은 사용자가 직접 구현

&nbsp;- 복잡한 로직에는 부적합함

●Semaphore

&nbsp;- 동시 접근 가능한 스레드 수를 제한하는 동기화 도구

&nbsp;- 제한된 개수만 동시에 접근 허용

&nbsp;- 동시에 사용가능한 자원 갯수 제한 가능

&nbsp;- 허용수 초과시 대기

●StampedLock

&nbsp;- 자바 8버전에 추가된 신규 기능

\- 재진입이 불가능한 락 (ReentrantLock과 달리 동일 스레드가 같은 락을 두번이상 걸 수 없음)

&nbsp;- 낙관적인 락을 사용

&nbsp;  => 락을 걸지않고 일단 읽기를 시도하고 나중에 데이터가 바뀌었는지 검증해서 바뀌었으면 락을 거는 방식

&nbsp;   => 읽기 속도가 매우 빠름

&nbsp;- 읽기위주의 상황에서 성능이 매우 좋음

