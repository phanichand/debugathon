from app.supplier import route_latency_ms


def test_supplier_latency_is_stable_for_repeated_customer_route():
    routes = [f'route-{i}' for i in range(1000)]
    first = [route_latency_ms(route) for route in routes]
    assert first == [route_latency_ms(route) for route in routes]
    assert set(first) == {60,600}
    assert .15 < first.count(600)/len(first) < .25
