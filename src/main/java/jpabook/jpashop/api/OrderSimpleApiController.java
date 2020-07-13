package jpabook.jpashop.api;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
