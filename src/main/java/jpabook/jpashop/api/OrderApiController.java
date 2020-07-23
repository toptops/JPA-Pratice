package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 컬렉션 조회..
 * 프레임워크 단에서 어짜피 조회 속도는 같다. 다만 이제 그걸 어떻게 매핑하고 처리하냐가 다르기 때문..
 *
 * 1. 직접 조회
 *  - 직접 Lazy Loading 하기 위해 엔티티를 전부 한번식 호출했다.
 *  - ... 가장 안좋은 방법이다. 엔티티도 api에 노출됨.
 *
 * 2. DTO로 변환해서 조회
 *  - DTO로 감싼다고 해서 엔티티를 노출해서는 안된다. (OrderItems 조차도 Dto로 전부 바꿔야한다.)
 *  - 역시나.. 조회 쿼리가 각각 나가서 쿼리수가 많아진다... (Lazy loading)
 *
 * 3. Fetch 조인
 *  - 실제 페치조인을 통해서 요청하는 값과 나오는 쿼리 값이 다르다..?
 *  - 페치조인을 이용하면 최적의 결과 값을 가져온다.
 *  - distinct 이용.. 1대다 조인이 있을시 row가 증가하는데 같은 엔티티도 증가하게 되는데, JPA distinct는 SQL에서
 *  distinct를 추가하여 쿼리를 실행해서 가져오는데, 여기에 더해서 한번더 중복을 걸러준다.
 *  - 페이징 쿼리는 되지 않는다.(메모리에서 처리해버려서 큰일난다)
 *  - 페치 조인시 실제 쿼리는 n * m 상황임으로 데이터가 늘어나게 되는데 여기서 페이징 처리하게되면 데이터를 이상하게 가져오게 되서 메모리에서 처리한다.
 *  (Order 기준으로 페이징이 되야하는데 OrderItem기준으로 들고오게됨)
 *  - 컬렉션 페치 조인을 2개이상 걸게 되면 1 * N * M이 되고 어떤 기준으로 가져오는지 정합성이 안맞게 된다.
 *
 *  4. 페이징과 한계 돌파(최적화) : 페이징 + 컬렉션 엔티티 조회 방법 설명(참조)
 *  - toOne 관계는 fetch조인을 사용한다. (하나의 데이터 단위만 조회함으로 toOne은 row수를 증가시키지 않기 때문.)
 *  - 컬렉션은 지연로딩으로 조회한다.
 *  - 지연 로딩 최적화를 위해 'hibernate.default_batch_fetch_size' 옵션 사용 : 미리 지정해 놓은 사이즈 만큼 데이터를 가져온다.
 *      - 쿼리 호출수가 줄어든다.
 *      - 패치 조인보다 데이터DB 전송량이 최적화 된다.
 *      - 데이터 양이 많다면 이 부분이 더 좋을 수 있다.(양이 적으면 fetch 나쁘지 않다)
 *      - 컬렉션 페치 조인은 페이징이 불가능하지만 이방법은 페이징이 가능
 *      - SQL IN절을 사용하기 때문에 100~1000 사이로 측정한다.
 *      - 메모리는.. 백만건 이상이면 쓰레드를 이용해서 데이터를 받는 즉시 처리도록 만들어야한다..
 *      (객체 천만개 생성해봤는데 cpu 점유율이 백프로를 찍었다.. 메모리는 무려 2G를 차지)
 *
 *  5. JPA에서 DTO 직접 조회
 *   - toOne 관계의 데이터를 가져온 이후 처리 방식
 *   - toOne 이후 toMany 값들을 따로 조회해서 가져온다
 *   - N + 1 문제 발생함(함수 확인)
 *  6. 5번 최적화
 *   - orderId로 ToMany 관계인 OrderItem을 한번에 조회(for문 한번 돌아야함..)
 *   - Map()을 사용하여 매칭 성능 높임
 *
 */
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();
            List<OrderItem> orderItems = order.getOrderItems();
            for (OrderItem orderItem : orderItems) {
                orderItem.getItem().getName();
            }
        }
        return all;
    }

    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
        return collect;
    }

    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
        return result;
    }

    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {

        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());

        return result;
    }

    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    @Data
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map( orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }

    }

    @Getter
    static class OrderItemDto {

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }

    }
}
