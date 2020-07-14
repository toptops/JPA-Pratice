package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Order 조회
 * Order -> Member
 * Order -> Delivery
 *
 * 1. 엔티티를 직접 노출 방법(안좋은 방법)
 *  - 엔티티를 직접 노출할 경우 성능, 유지보수에 많은 문제가 있다. (API 스펙이 바뀌거나 도메인이 바뀔경우 전체적으로 다 바꿔야함)
 *  - 또한 직접 넘길 경우 또한 JSON 배열로 넘어가기에 확장성에도 문제가 있다.
 *  - 그리고 무한루프 문제.. JsonIgnore도 설정해줘야하고, 그리고 지연로딩때문에 데이터가 안넘어간다.(조회를 안했기 때문, 강제 로딩 or 직접 로딩 코드 작성)
 *
 * 2. 응답 객체 생성 방법.
 *  - 응답 객체를 생성하여 확장성 있고 유연하게 개발이 가능하다.
 *  - 응답 객체를 생성한다고 하더라도 지연로딩이 발생함. N + 1 문제 발생.
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName();
            order.getOrderItems().get(0).getOrderPrice();
        }
        return all;
    }

    /*
     * N + 1 문제,  1번의 조회 결과 N개가 나왔지만 다른 테이블에도 N번 조회가 필요함.
     * 1. Order 1번 조회 -> 결과 2개
     * 2. Order -> SimpleOrderDto 변환 중에, member와 delivery 테이블에 지연로딩을 요청
     * 3. 2N + 1 문제 발생(최악) - 지연로딩은 기본적으로 영속성 컨텍스트를 조회한다.
     *
     * @return
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        return orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            this.orderId = order.getId();
            this.name = order.getMember().getName(); //LAZY
            this.orderDate = order.getOrderDate();
            this.orderStatus = order.getStatus();
            this.address = order.getMember().getAddress(); //LAZY
        }

    }

}
