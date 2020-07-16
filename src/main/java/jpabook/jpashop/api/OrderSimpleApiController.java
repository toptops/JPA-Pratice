package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryRepository;
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
 *
 * 3. fetch join
 *  - 페치조인의 특징은 원하는 엔티티에 대해서 Lazy 로딩 방식 아니고 조회할때 한번에 들고온다.(진짜 조회해서 들고옴)
 *  - 기존 2번 방식에서는 조회되는 쿼리수가 많았지만 페치조인을 통해서 1번의 쿼리로 모든 정보들을 들고온다.
 *  - 페치조인도 단점이 있는데 셀렉트로 엔티티를 전부 찍어서 조회하는(?)
 *  - 페치조인은 실무에서 쓰려면 잘 알야한다 <- 별 5개짜리
 *
 * 4. V4와 V3 차이
 *  - 성능 차이는 별로 사실 없다..
 *  - 실시간으로 자주 사용 한다면 (트래픽이 높게 되면) ->  V4 처럼 Dto를 받아서 최적화를 고려해야한다.
 *  - Dto 자체를 받아옴으로, 다른 곳에서 재사용이 힘들다.(api spec 자체를 받아옴으로..)
 *  - v3에서는 엔티티 자체를 받아옴으로 다른곳에서도 재사용이 가능함..
 *  - 기본 Repository에서는 Entity 위주로 뽑아내고 복잡한 쿼리같은 경우는 따로 Repository를 분리해서 만든다.(권장)
 *
 *  쿼리 방식 선택 권장 순서
 *  1. 우선 엔티티를 DTO로 변환하는 방법을 선택
 *  2. 필요하면 Fetch조인으로 성능을 최적화 -> 대부분이 해결된다.
 *  3. 그래도 안되면 DTO로 직접 조회하는 방법을 사용
 *  4. 최후의 방법은 JPA가 제공하는 네이티브 SQL이나 스프링 JDBC Template를 사용해서 SQL을 사용.
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

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

    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> orderV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<SimpleOrderDto> result = orders.stream()
                                        .map(o -> new SimpleOrderDto(o))
                                        .collect(Collectors.toList());

        return result;
    }

    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> orderV4() {
       return orderSimpleQueryRepository.findOrderDtos();
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
